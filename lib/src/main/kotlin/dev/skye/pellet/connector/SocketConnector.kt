package dev.skye.pellet.connector

import dev.skye.pellet.CloseReason
import dev.skye.pellet.PelletClient
import dev.skye.pellet.codec.Codec
import dev.skye.pellet.extension.awaitRead
import dev.skye.pellet.logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.io.IOException
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.ClosedChannelException

class SocketConnector(
    private val scope: CoroutineScope,
    private val socketAddress: SocketAddress,
    private val codecFactory: (PelletClient) -> Codec
) : Connector {

    override fun createAcceptJob() = createSocketAcceptJob(scope, socketAddress, this::launchReadLoop)

    private fun launchReadLoop(
        socketChannel: AsynchronousSocketChannel
    ) = scope.launch {
        val client = PelletClient(socketChannel)
        val codec = codecFactory(client)
        readLoop(client, codec, socketChannel, bufferSize = 1024)
    }

    private suspend fun CoroutineScope.readLoop(
        client: PelletClient,
        codec: Codec,
        socketChannel: AsynchronousSocketChannel,
        bufferSize: Int
    ) {
        val buffer = ByteBuffer.allocateDirect(bufferSize)
        while (this.isActive) {
            val numberBytesRead = try {
                socketChannel.awaitRead(buffer)
            } catch (exception: ClosedChannelException) {
                client.close(CloseReason.ServerException(exception))
                return
            } catch (exception: IOException) {
                logger.warn("failed to read $socketChannel", exception)
                client.close(CloseReason.ServerException(exception))
                continue
            }
            if (numberBytesRead < 0) {
                client.close(CloseReason.ClientInitiated)
                return
            }

            val bytesToConsume = buffer.flip().asReadOnlyBuffer()
            codec.consume(bytesToConsume)
            buffer.clear()

            yield()
        }
    }
}
