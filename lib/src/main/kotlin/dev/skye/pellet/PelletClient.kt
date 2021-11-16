package dev.skye.pellet

import dev.skye.pellet.extension.awaitWrite
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel

sealed class CloseReason {

    object ClientInitiated : CloseReason()
    object ServerInitiated : CloseReason()
    data class ServerException(val throwable: Throwable) : CloseReason()
}

class PelletClient(
    private val socket: AsynchronousSocketChannel
) {
    suspend fun write(bytes: ByteBuffer): Int {
        return socket.awaitWrite(bytes)
    }

    fun close(source: CloseReason) {
        logger.debug("closed $socket (initiator: $source)")
        socket.close()
    }
}
