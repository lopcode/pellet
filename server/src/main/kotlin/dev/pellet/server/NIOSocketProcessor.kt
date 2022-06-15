package dev.pellet.server

import dev.pellet.logging.pelletLogger
import dev.pellet.server.buffer.PelletBuffer
import dev.pellet.server.buffer.PelletBufferPooling
import kotlinx.coroutines.CoroutineScope
import java.net.SocketException
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector

internal class NIOSocketProcessor(
    private val pool: PelletBufferPooling,
    private val readSelector: Selector,
    private val scope: CoroutineScope
) {

    private val buffer = pool.provide()
    private val logger = pelletLogger<NIOSocketProcessor>()

    fun run() {
        while (!Thread.currentThread().isInterrupted) {
            readAll()
        }
    }

    private fun readAll(): Int {
        val numberKeysReady = this.readSelector.select()
        if (numberKeysReady <= 0) {
            return 0
        }

        val selectedKeys = this.readSelector.selectedKeys()
        val iterator = selectedKeys.iterator()
        while (iterator.hasNext()) {
            val key = iterator.next()
            iterator.remove()
            buffer.clear()
            readSocket(key, buffer)
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
        // val timer = PelletTimer()
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
        // val timeToRead = timer.markAndReset()
        // val bytesToConsume = buffer.flip()
        val bytesCopy = clone(buffer.byteBuffer)
        val newBuffer = PelletBuffer(bytesCopy).flip()
        client.trackedSocket.codec.consume(newBuffer, client)
        // val timeToProcess = timer.markAndReset()
        // val timeToReadMs = timeToRead.toMillis()
        // val timeToProcessMs = timeToProcess.toMillis()
        // if (timeToReadMs > 1 || timeToProcessMs > 1) {
        //     logger.info { "read and process time: ${timeToReadMs}ms ${timeToProcessMs}ms" }
        // }
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
