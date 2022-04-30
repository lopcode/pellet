package dev.pellet.server.connector

import dev.pellet.logging.pelletLogger
import dev.pellet.server.extension.awaitAccept
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.net.SocketAddress
import java.nio.channels.AsynchronousChannelGroup
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel

private val logger = pelletLogger("Connector")

interface Connector {

    fun createAcceptJob(): Job
}

fun createSocketAcceptJob(
    scope: CoroutineScope,
    group: AsynchronousChannelGroup,
    socketAddress: SocketAddress,
    launchReadLoop: suspend (AsynchronousSocketChannel) -> Unit
): Job {
    val unboundSocket = AsynchronousServerSocketChannel.open(group)
    val serverSocketChannel = unboundSocket.bind(socketAddress)
    logger.debug { "socket opened: $socketAddress" }

    return scope.launch(
        start = CoroutineStart.LAZY
    ) {
        while (this.isActive) {
            accept(
                serverSocketChannel,
                launchReadLoop
            )
            yield()
        }
    }
}

private suspend fun accept(
    serverSocketChannel: AsynchronousServerSocketChannel,
    launchReadLoop: suspend (AsynchronousSocketChannel) -> Unit
) {
    val socketChannel = runCatching {
        serverSocketChannel.awaitAccept()
    }.getOrElse {
        logger.warn(it) { "failed to accept connection" }
        return
    }
    launchReadLoop(socketChannel)
    logger.debug { "accepted: $socketChannel" }
}
