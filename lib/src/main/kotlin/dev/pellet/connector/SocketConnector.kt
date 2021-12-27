package dev.pellet.connector

import dev.pellet.CloseReason
import dev.pellet.PelletClient
import dev.pellet.buffer.PelletBufferPooling
import dev.pellet.codec.Codec
import dev.pellet.extension.awaitRead
import dev.pellet.logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.io.IOException
import java.net.SocketAddress
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.ClosedChannelException

class SocketConnector(
    private val scope: CoroutineScope,
    private val socketAddress: SocketAddress,
    private val pool: PelletBufferPooling,
    private val codecFactory: (PelletClient) -> Codec
) : Connector {

    override fun createAcceptJob() = createSocketAcceptJob(
        scope,
        socketAddress,
        this::launchReadLoop
    )

    private fun launchReadLoop(
        socketChannel: AsynchronousSocketChannel
    ) = scope.launch {
        val client = PelletClient(socketChannel, pool)
        val codec = codecFactory(client)
        readLoop(client, codec, socketChannel)
    }

    private suspend fun CoroutineScope.readLoop(
        client: PelletClient,
        codec: Codec,
        socketChannel: AsynchronousSocketChannel
    ) {
        val buffer = pool.provide()
        while (this.isActive) {
            val numberBytesRead = try {
                socketChannel.awaitRead(buffer)
            } catch (exception: ClosedChannelException) {
                close(
                    client,
                    codec,
                    CloseReason.ServerException(exception)
                )
                return
            } catch (exception: IOException) {
                logger.warn("failed to read $socketChannel", exception)
                close(
                    client,
                    codec,
                    CloseReason.ServerException(exception)
                )
                return
            }
            if (numberBytesRead < 0) {
                close(
                    client,
                    codec,
                    CloseReason.ClientInitiated
                )
                return
            }

            val bytesToConsume = buffer.flip()
            codec.consume(bytesToConsume)
            buffer.clear()

            yield()
        }
    }

    private fun close(
        client: PelletClient,
        codec: Codec,
        reason: CloseReason
    ) {
        client.close(reason)
        codec.release()
    }
}
