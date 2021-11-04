package dev.skye.pellet

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import kotlin.concurrent.thread
import kotlin.coroutines.CoroutineContext
import kotlin.math.max

object Demo

val logger = logger<Demo>()
class PelletClient(
    val socket: SocketChannel
) {
    lateinit var codec: HTTPMessageCodec
}

data class PelletRequest(
    val message: HTTPRequestMessage,
    val client: PelletClient
)

fun main() = runBlocking {
    val unboundSocket = ServerSocketChannel.open()
    val serverSocketChannel = unboundSocket.bind(InetSocketAddress("localhost", 8082))
    logger.info("socket opened: $serverSocketChannel")
    val acceptSelector = Selector.open()
    serverSocketChannel.configureBlocking(false)
    serverSocketChannel.register(acceptSelector, SelectionKey.OP_ACCEPT)

    val numberOfReaders = max(1, Runtime.getRuntime().availableProcessors())

    val context = SupervisorJob()
    val scope = object : CoroutineScope {
        override val coroutineContext: CoroutineContext
            get() = Dispatchers.Default + context
    }

    val readSelectors = (0 until numberOfReaders).map {
        Selector.open()
    }

    val acceptThread = thread(start = true) {
        acceptLoop(acceptSelector, readSelectors, scope, serverSocketChannel)
    }

    val readThreads = readSelectors.map { selector ->
        thread(start = true) {
            readLoop(selector, 1024)
        }
    }

    readThreads.forEach { it.join() }
    acceptThread.join()
}

private fun acceptLoop(
    selector: Selector,
    readSelectors: List<Selector>,
    scope: CoroutineScope,
    serverSocketChannel: ServerSocketChannel
) {
    val numberOfReadSelectors = readSelectors.size
    var currentReadSelector = 0
    while (!Thread.currentThread().isInterrupted) {
        val numberSelected = selector.select()
        if (numberSelected <= 0) {
            continue
        }

        val keys = selector.selectedKeys()
        val iterator = keys.iterator()
        while (iterator.hasNext()) {
            val key = iterator.next()
            iterator.remove()

            if (key.isValid && key.isAcceptable) {
                val socketChannel = try {
                    serverSocketChannel.accept()
                } catch (exception: IOException) {
                    logger.error("failed to accept connection", exception)
                    break
                }
                socketChannel.configureBlocking(false)
                // todo: how to track clients - be able to go socket <-> client
                val client = PelletClient(socketChannel)
                val output = HTTPMessageCodecOutput(scope, client)
                client.codec = HTTPMessageCodec(output)
                socketChannel.register(readSelectors[currentReadSelector], SelectionKey.OP_READ, client)
                readSelectors[currentReadSelector].wakeup()
                currentReadSelector++
                if (currentReadSelector == numberOfReadSelectors) {
                    currentReadSelector = 0
                }

                logger.info("accepted: $socketChannel")
            }
        }
    }
}

private fun readLoop(
    selector: Selector,
    bufferSize: Int
) {
    val buffer = ByteBuffer.allocateDirect(bufferSize)
    while (!Thread.currentThread().isInterrupted) {
        val numberSelected = selector.select() // todo: await
        if (numberSelected <= 0) {
            continue
        }

        val keys = selector.selectedKeys()
        val iterator = keys.iterator()
        while (iterator.hasNext()) {
            val key = iterator.next()
            iterator.remove()

            if (key.isValid && key.isReadable) {
                val socketChannel = key.channel() as SocketChannel
                val client = key.attachment() as? PelletClient
                if (client == null) {
                    logger.warn("failed to get attachment $socketChannel")
                    key.cancel()
                    socketChannel.close()
                    continue
                }
                val numberBytesRead = try {
                    socketChannel.read(buffer) // todo: await
                } catch (exception: IOException) {
                    logger.warn("failed to read $socketChannel", exception)
                    key.attach(null)
                    key.cancel()
                    socketChannel.close()
                    continue
                }
                if (numberBytesRead < 0) {
                    logger.info("EOF: $socketChannel")
                    key.attach(null)
                    key.cancel()
                    socketChannel.close()
                    continue
                }

                val bytesToConsume = buffer.flip().asReadOnlyBuffer()
                client.codec.consume(bytesToConsume)
                buffer.clear()

                writeNoContent(socketChannel)
            }
        }
    }
}

private fun writeNoContent(socketChannel: SocketChannel) {
    val noContent = "HTTP/1.1 204 No Content\r\n\r\n"
    val bytes = Charsets.US_ASCII.encode(noContent)
    try {
        socketChannel.write(bytes) // todo: await
    } catch (exception: IOException) {
        // ignore
    }
}
