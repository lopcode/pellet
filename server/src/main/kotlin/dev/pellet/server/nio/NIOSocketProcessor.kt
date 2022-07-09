package dev.pellet.server.nio

import dev.pellet.server.PelletServerClient
import dev.pellet.server.buffer.PelletBuffer
import dev.pellet.server.buffer.PelletBufferPooling
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import java.net.SocketException
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector

internal class NIOSocketProcessor(
    pool: PelletBufferPooling,
    private val selector: Selector
) {

    private val buffer = pool.provide()

    fun run(scope: CoroutineScope) = scope.launch(
        start = CoroutineStart.LAZY,
        context = CoroutineName("nio processor")
    ) {
        while (isActive) {
            processSockets()
        }
    }

    private suspend fun processSockets(): Int {
        val numberKeysReady = runInterruptible {
            this.selector.select()
        }
        if (numberKeysReady <= 0) {
            return 0
        }

        val selectedKeys = this.selector.selectedKeys()
        val iterator = selectedKeys.iterator()
        while (iterator.hasNext()) {
            val key = iterator.next()
            iterator.remove()
            if (!key.isValid) {
                continue
            }
            if (key.isValid && key.isReadable) {
                buffer.clear()
                readSocket(key, buffer)
            }
        }

        return numberKeysReady
    }

    private fun readSocket(
        key: SelectionKey,
        buffer: PelletBuffer
    ) {
        val client = key.attachment() as? PelletServerClient
        if (client == null) {
            // too fast to read - skip this client for now
            return
        }
        val bytesRead = try {
            client.trackedSocket.channel.read(buffer.byteBuffer)
        } catch (exception: SocketException) {
            close(key)
            return
        }
        if (bytesRead < 0) {
            // socket closed - client initiated
            close(key)
            return
        }
        val bytesCopy = clone(buffer.byteBuffer)
        val newBuffer = PelletBuffer(bytesCopy).flip()
        client.trackedSocket.codec.consume(newBuffer, client)
    }

    private fun clone(original: ByteBuffer): ByteBuffer {
        val copy = ByteBuffer.allocate(original.capacity())
        val readOnlyView = original.asReadOnlyBuffer()
        readOnlyView.flip()
        copy.put(readOnlyView)
        return copy
    }

    private fun close(
        key: SelectionKey
    ) {
        val trackedClient = (key.attachment() as PelletServerClient)
        key.attach(null)
        key.cancel()
        trackedClient.trackedSocket.channel.close()
    }
}
