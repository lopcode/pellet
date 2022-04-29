package dev.pellet.server

import dev.pellet.logging.debug
import dev.pellet.logging.pelletLogger
import dev.pellet.server.buffer.PelletBuffer
import dev.pellet.server.buffer.PelletBufferPooling
import java.net.InetSocketAddress
import java.net.UnixDomainSocketAddress
import java.nio.channels.SocketChannel

sealed class CloseReason {

    object ClientInitiated : CloseReason()
    object ServerInitiated : CloseReason()
    data class ServerException(val throwable: Throwable) : CloseReason()
}

class PelletServerClient(
    private val socket: SocketChannel,
    private val pool: PelletBufferPooling
) {

    private val logger = pelletLogger<PelletServerClient>()

    val remoteHostString: String
        get() {
            return when (val remoteAddress = socket.remoteAddress) {
                is InetSocketAddress -> remoteAddress.hostString
                is UnixDomainSocketAddress -> remoteAddress.path.toString()
                else -> remoteAddress.toString()
            }
        }

    fun writeAndRelease(buffer: PelletBuffer): Result<Int> {
        val result = runCatching {
            socket.write(buffer.byteBuffer)
        }
        pool.release(buffer)
        return result
    }

    fun close(source: CloseReason): Result<Unit> {
        logger.debug { "closed $socket (initiator: $source)" }
        return runCatching {
            socket.close()
        }
    }
}
