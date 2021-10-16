package dev.skye.pellet

import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel

object Demo

fun main() {
    val logger = logger<Demo>()

    val unboundSocket = ServerSocketChannel.open()
    val serverSocketChannel = unboundSocket.bind(InetSocketAddress("localhost", 8082))
    logger.info("socket opened: $serverSocketChannel")
    val selector = Selector.open()
    serverSocketChannel.configureBlocking(false)
    serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT)

    val codec = HTTPMessageCodec()

    val buffer = ByteBuffer.allocate(1024)
    buffer.clear()

    loop@ while (true) {
        val numberSelected = selector.select()
        if (numberSelected <= 0) {
            continue@loop
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
                    break@loop
                }
                socketChannel.configureBlocking(false)
                socketChannel.register(selector, SelectionKey.OP_READ or SelectionKey.OP_WRITE)

                logger.info("accepted: $socketChannel")
            }

            if (key.isValid && key.isReadable) {
                val socketChannel = key.channel() as SocketChannel
                val numberBytesRead = try {
                    socketChannel.read(buffer)
                } catch (exception: IOException) {
                    logger.warn("failed to read $socketChannel", exception)
                    key.cancel()
                    socketChannel.close()
                    continue@loop
                }
                if (numberBytesRead < 0) {
                    logger.info("EOF: $socketChannel")
                    key.cancel()
                    socketChannel.close()
                    continue@loop
                }

                val bytesToConsume = buffer
                    .slice(buffer.arrayOffset(), numberBytesRead)
                    .asReadOnlyBuffer()
                val messages = codec.consume(bytesToConsume)
                buffer.clear()
                if (messages.isEmpty()) {
                    continue@loop
                }

                logger.info("got messages: $messages")

                // todo: send a dummy reply back to any message
                val noContent = "HTTP/1.1 204 No Content\r\n\r\n"
                val bytes = Charsets.US_ASCII.encode(noContent)
                try {
                    socketChannel.write(bytes)
                } catch (exception: IOException) {
                    // ignore
                }
            }

            // todo: test if there are any outgoing messages to send for this socket
            if (key.isValid && key.isWritable && false) {
                logger.info("$key writable")
            }
        }
    }
}
