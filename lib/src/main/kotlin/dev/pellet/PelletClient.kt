package dev.pellet

import dev.pellet.buffer.PelletBuffer
import dev.pellet.buffer.PelletBufferPooling
import dev.pellet.extension.awaitWrite
import java.nio.channels.AsynchronousSocketChannel

sealed class CloseReason {

    object ClientInitiated : CloseReason()
    object ServerInitiated : CloseReason()
    data class ServerException(val throwable: Throwable) : CloseReason()
}

class PelletClient(
    private val socket: AsynchronousSocketChannel,
    private val pool: PelletBufferPooling
) {

    suspend fun writeAndRelease(buffer: PelletBuffer): Result<Int> {
        val result = runCatching {
            socket.awaitWrite(buffer.byteBuffer)
        }
        pool.release(buffer)
        return result
    }

    fun close(source: CloseReason): Result<Unit> {
        logger.debug("closed $socket (initiator: $source)")
        return runCatching {
            socket.close()
        }
    }
}
