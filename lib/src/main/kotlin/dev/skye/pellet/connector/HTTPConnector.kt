package dev.skye.pellet.connector

import dev.skye.pellet.CloseReason
import dev.skye.pellet.PelletClient
import dev.skye.pellet.PelletContext
import dev.skye.pellet.PelletResponder
import dev.skye.pellet.codec.http.HTTPMessageCodec
import dev.skye.pellet.codec.http.HTTPRequestHandler
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

class HTTPConnector(
    private val scope: CoroutineScope,
    private val socketAddress: SocketAddress,
    private val action: suspend (PelletContext, PelletResponder) -> Unit
) : Connector {

    override fun createAcceptJob() = createSocketAcceptJob(scope, socketAddress, this::launchReadLoop)

    private fun launchReadLoop(
        socketChannel: AsynchronousSocketChannel
    ) = scope.launch {
        val client = PelletClient(socketChannel)
        val output = HTTPRequestHandler(client, action)
        val codec = HTTPMessageCodec(output)
        client.codec = codec
        readLoop(client, socketChannel, bufferSize = 1024)
    }

    private suspend fun CoroutineScope.readLoop(
        client: PelletClient,
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
            client.codec.consume(bytesToConsume)
            buffer.clear()

            yield()
        }
    }
}
