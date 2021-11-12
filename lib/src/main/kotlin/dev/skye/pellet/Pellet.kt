package dev.skye.pellet

import dev.skye.pellet.codec.http.HTTPMessageCodec
import dev.skye.pellet.codec.http.HTTPMessageCodecOutput
import dev.skye.pellet.extension.awaitAccept
import dev.skye.pellet.extension.awaitRead
import dev.skye.pellet.logging.logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.scheduling.ExperimentalCoroutineDispatcher
import kotlinx.coroutines.yield
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousChannelGroup
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.ClosedChannelException
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

val logger = logger<Pellet>()

class Pellet(
    private val hostname: String,
    private val port: Int
) {

    fun start(
        action: suspend (PelletContext, PelletResponder) -> Unit
    ): Job {
        val processors = Runtime.getRuntime().availableProcessors().coerceAtLeast(2)
        val group = AsynchronousChannelGroup.withThreadPool(Executors.newFixedThreadPool(2))
        val unboundSocket = AsynchronousServerSocketChannel.open(group)
        val serverSocketChannel = unboundSocket.bind(InetSocketAddress(hostname, port))
        logger.info("socket opened: $serverSocketChannel")

        @OptIn(InternalCoroutinesApi::class)
        val dispatcher = ExperimentalCoroutineDispatcher().limited(processors)
        val context = SupervisorJob()
        val scope = object : CoroutineScope {
            override val coroutineContext: CoroutineContext
                get() = dispatcher + context
        }

        val acceptJob = scope.launch {
            acceptLoop(scope, serverSocketChannel) {
                val output = HTTPMessageCodecOutput(it, action)
                it.codec = HTTPMessageCodec(output)
            }
        }

        acceptJob.start()
        return acceptJob
    }
}

private suspend fun CoroutineScope.acceptLoop(
    readScope: CoroutineScope,
    serverSocketChannel: AsynchronousServerSocketChannel,
    codecDecorator: (PelletClient) -> Unit
) {
    while (this.isActive) {
        val socketChannel = try {
            serverSocketChannel.awaitAccept()
        } catch (exception: IOException) {
            logger.error("failed to accept connection", exception)
            break
        }

        val client = PelletClient(socketChannel)
        codecDecorator(client)
        readScope.launch {
            readLoop(client, socketChannel, bufferSize = 1024)
        }

        logger.info("accepted: $socketChannel")
        yield()
    }
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
            return
        } catch (exception: IOException) {
            logger.warn("failed to read $socketChannel", exception)
            socketChannel.close()
            continue
        }
        if (numberBytesRead < 0) {
            logger.info("EOF: $socketChannel")
            socketChannel.close()
            return
        }

        val bytesToConsume = buffer.flip().asReadOnlyBuffer()
        client.codec.consume(bytesToConsume)
        buffer.clear()

        yield()
    }
}
