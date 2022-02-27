package dev.pellet.server.connector

import dev.pellet.logging.pelletLogger
import dev.pellet.server.extension.awaitAccept
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.io.IOException
import java.net.SocketAddress
import java.nio.channels.AsynchronousChannelGroup
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.util.concurrent.Executors

private val logger = pelletLogger("Connector")

interface Connector {

    fun createAcceptJob(): Job
}

fun createSocketAcceptJob(
    scope: CoroutineScope,
    socketAddress: SocketAddress,
    launchReadLoop: suspend (AsynchronousSocketChannel) -> Unit
): Job {
    val group = AsynchronousChannelGroup.withThreadPool(Executors.newFixedThreadPool(2))
    val unboundSocket = AsynchronousServerSocketChannel.open(group)
    val serverSocketChannel = unboundSocket.bind(socketAddress)
    logger.debug("socket opened: $socketAddress")

    return scope.launch(
        start = CoroutineStart.LAZY
    ) {
        while (this.isActive) {
            val socketChannel = try {
                serverSocketChannel.awaitAccept()
            } catch (exception: IOException) {
                logger.error("failed to accept connection", exception)
                break
            }

            launchReadLoop(socketChannel)

            logger.debug("accepted: $socketChannel")
            yield()
        }
    }
}
