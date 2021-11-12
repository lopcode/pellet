package dev.skye.pellet

import dev.skye.pellet.codec.Codec
import dev.skye.pellet.extension.awaitWrite
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel

class PelletClient(
    private val socket: AsynchronousSocketChannel
) {
    lateinit var codec: Codec

    suspend fun write(bytes: ByteBuffer): Int {
        return socket.awaitWrite(bytes)
    }

    fun close() {
        logger.info("closed $socket")
        socket.close()
    }
}
