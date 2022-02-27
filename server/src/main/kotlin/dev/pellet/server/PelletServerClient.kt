package dev.pellet.server

import dev.pellet.logging.pelletLogger
import dev.pellet.server.buffer.PelletBuffer
import dev.pellet.server.buffer.PelletBufferPooling
import dev.pellet.server.extension.awaitWrite
import java.nio.channels.AsynchronousSocketChannel

sealed class CloseReason {

    object ClientInitiated : CloseReason()
    object ServerInitiated : CloseReason()
    data class ServerException(val throwable: Throwable) : CloseReason()
}

class PelletServerClient(
    private val socket: AsynchronousSocketChannel,
    private val pool: PelletBufferPooling
) {

    private val logger = pelletLogger<PelletServerClient>()

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
