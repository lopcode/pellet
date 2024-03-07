package dev.pellet.server.socket

import dev.pellet.logging.pelletLogger
import dev.pellet.server.CloseReason
import dev.pellet.server.PelletServerClient
import dev.pellet.server.codec.Codec
import kotlinx.io.Buffer
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import java.net.InetSocketAddress
import java.net.Socket
import java.net.UnixDomainSocketAddress
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

data class BlockingPelletServerClient(
    internal val socket: Socket,
    override val codec: Codec
) : PelletServerClient {

    private val logger = pelletLogger<PelletServerClient>()
    private val source = socket.getInputStream().asSource().buffered()
    private val sink = socket.getOutputStream().asSink().buffered()
    private val readLock = ReentrantLock()
    private val writeLock = ReentrantLock()

    companion object {

        const val MAX_READ_SIZE = 8192L
    }

    override val remoteHostString: String
        get() {
            return when (val remoteAddress = socket.remoteSocketAddress) {
                is InetSocketAddress -> remoteAddress.hostString
                is UnixDomainSocketAddress -> remoteAddress.path.toString()
                else -> remoteAddress.toString()
            }
        }

    override fun write(vararg buffers: Buffer): Result<Long> = writeLock.withLock {
        var bytesWritten = 0L
        buffers.forEach {
            val attempt = runCatching {
                bytesWritten += it.transferTo(sink)
                bytesWritten
            }
            if (attempt.isFailure) {
                return attempt
            }
        }
        return runCatching {
            sink.flush()
            bytesWritten
        }
    }

    override fun write(buffer: Buffer): Result<Long> = writeLock.withLock {
        var bytesWritten = 0L
        val attempt = runCatching {
            bytesWritten += buffer.transferTo(sink)
            bytesWritten
        }
        if (attempt.isFailure) {
            return attempt
        }
        return runCatching {
            sink.flush()
            bytesWritten
        }
    }

    override fun read(buffer: Buffer): Result<Long> = readLock.withLock {
        return runCatching {
            source.readAtMostTo(buffer, MAX_READ_SIZE)
        }
    }

    override fun close(reason: CloseReason): Result<Unit> {
        logger.debug { "closed ${socket.remoteSocketAddress} (initiator: $reason)" }
        return runCatching {
            socket.close()
        }
    }
}
