package dev.pellet.server.connector

import dev.pellet.logging.debug
import dev.pellet.logging.pelletLogger
import dev.pellet.logging.warn
import java.net.SocketAddress
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService

private val logger = pelletLogger("Connector")

interface Connector {

    fun createAcceptJob(): CompletableFuture<Void>
}

fun createSocketAcceptJob(
    executorService: ExecutorService,
    socketAddress: SocketAddress,
    launchReadLoop: (SocketChannel) -> Unit
): CompletableFuture<Void> {
    val unboundSocket = ServerSocketChannel.open()
    val serverSocketChannel = unboundSocket.bind(socketAddress)
    logger.debug { "socket opened: $socketAddress" }

    return CompletableFuture.supplyAsync({
        while (!Thread.currentThread().isInterrupted) {
            accept(
                serverSocketChannel,
                launchReadLoop
            )
        }
        null
    }, executorService)
}

private fun accept(
    serverSocketChannel: ServerSocketChannel,
    launchReadLoop: (SocketChannel) -> Unit
): Void? {
    val socketChannel = runCatching {
        serverSocketChannel.accept()
    }.getOrElse {
        logger.warn(it) { "failed to accept connection" }
        return null
    }
    launchReadLoop(socketChannel)
    logger.debug { "accepted: $socketChannel" }
    return null
}
