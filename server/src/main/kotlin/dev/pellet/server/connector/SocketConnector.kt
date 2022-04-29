package dev.pellet.server.connector

import dev.pellet.logging.pelletLogger
import dev.pellet.server.CloseReason
import dev.pellet.server.PelletServerClient
import dev.pellet.server.buffer.PelletBufferPooling
import dev.pellet.server.codec.Codec
import kotlinx.coroutines.CoroutineScope
import java.net.SocketAddress
import java.nio.channels.SocketChannel
import java.util.concurrent.ExecutorService

class SocketConnector(
    private val scope: CoroutineScope,
    private val executorService: ExecutorService,
    private val socketAddress: SocketAddress,
    private val pool: PelletBufferPooling,
    private val codecFactory: (PelletServerClient) -> Codec
) : Connector {

    private val logger = pelletLogger<SocketConnector>()

    override fun createAcceptJob() = createSocketAcceptJob(
        executorService,
        socketAddress,
        this::launchReadLoop
    )

    private fun launchReadLoop(
        socketChannel: SocketChannel
    ) = executorService.execute {
        val client = PelletServerClient(socketChannel, pool)
        val codec = codecFactory(client)
        readLoop(client, codec, socketChannel)
    }

    private fun readLoop(
        client: PelletServerClient,
        codec: Codec,
        socketChannel: SocketChannel
    ) {
        val buffer = pool.provide()
        while (!Thread.currentThread().isInterrupted) {
            val numberBytesRead = this@SocketConnector.runCatching {
                socketChannel.read(buffer.byteBuffer)
            }.getOrElse {
                close(
                    client,
                    codec,
                    CloseReason.ServerException(it)
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
        }
    }

    private fun close(
        client: PelletServerClient,
        codec: Codec,
        reason: CloseReason
    ) {
        client.close(reason)
        codec.release()
    }
}
