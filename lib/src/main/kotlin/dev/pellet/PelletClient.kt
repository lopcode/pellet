package dev.pellet

import dev.pellet.extension.awaitWrite
import java.nio.channels.AsynchronousSocketChannel

sealed class CloseReason {

    object ClientInitiated : CloseReason()
    object ServerInitiated : CloseReason()
    data class ServerException(val throwable: Throwable) : CloseReason()
}

class PelletClient(
    private val socket: AsynchronousSocketChannel,
    private val pool: PelletBufferPool
) {

    suspend fun write(buffer: PelletBuffer) {
        socket.awaitWrite(buffer.byteBuffer)
        pool.release(buffer)
    }

    fun close(source: CloseReason): Result<Unit> {
        logger.debug("closed $socket (initiator: $source)")
        return runCatching {
            socket.close()
        }
    }
}
