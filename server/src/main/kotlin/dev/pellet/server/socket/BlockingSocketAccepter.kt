package dev.pellet.server.socket

import dev.pellet.logging.pelletLogger
import dev.pellet.server.codec.Codec
import java.net.ServerSocket
import java.net.SocketAddress
import java.net.StandardSocketOptions.SO_REUSEADDR
import java.net.StandardSocketOptions.SO_REUSEPORT
import java.util.concurrent.ExecutorService
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.Semaphore

class BlockingSocketAccepter(
    private val executorService: ExecutorService,
    private val handlerFactory: () -> BlockingSocketHandler,
    connectionConcurrencyLimit: Int
) {

    private val logger = pelletLogger<BlockingSocketAccepter>()
    private val semaphore = Semaphore(connectionConcurrencyLimit)

    fun accept(
        socketAddress: SocketAddress,
        codecFactory: () -> Codec
    ) {
        Thread.currentThread().name = "pellet-accepter"
        val serverSocket = ServerSocket()
        serverSocket.bind(socketAddress)
        serverSocket.setOption(SO_REUSEADDR, true)
        serverSocket.setOption(SO_REUSEPORT, true)
        serverSocket.use {
            while (!Thread.currentThread().isInterrupted) {
                val rawSocket = serverSocket.accept()
                val codec = codecFactory()
                val client = BlockingPelletServerClient(rawSocket, codec)

                logger.debug { "accepted $client" }
                try {
                    semaphore.acquire()
                    executorService.execute {
                        val socketHandler = runCatching {
                            handlerFactory()
                        }.getOrElse {
                            logger.error(it) { "failed to make socket handler" }
                            semaphore.release()
                            return@execute
                        }
                        socketHandler.handle(client)
                        semaphore.release()
                        logger.debug { "ended $client" }
                    }
                } catch (exception: RejectedExecutionException) {
                    semaphore.drainPermits()
                    logger.error(exception) { "failed to handle socket" }
                    return@use
                } catch (exception: InterruptedException) {
                    semaphore.drainPermits()
                    logger.debug { "handling interrupted for $client" }
                    Thread.currentThread().interrupt()
                    return@use
                }
            }
        }
    }
}
