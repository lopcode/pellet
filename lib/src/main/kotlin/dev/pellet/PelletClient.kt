package dev.pellet

import dev.pellet.extension.awaitWrite
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
    suspend fun write(bytes: ByteBuffer): Result<Int> {
        return runCatching {
            socket.awaitWrite(bytes)
        }
    }

    fun close(source: CloseReason): Result<Unit> {
        logger.debug("closed $socket (initiator: $source)")
        return runCatching {
            socket.close()
        }
    }
}
