package dev.pellet.server.nio

import dev.pellet.logging.pelletLogger
import dev.pellet.server.CloseReason
import dev.pellet.server.PelletServerClient
import dev.pellet.server.buffer.PelletBuffer
import dev.pellet.server.buffer.PelletBufferPooling
import dev.pellet.server.codec.Codec
import java.net.InetSocketAddress
import java.net.UnixDomainSocketAddress

class NIOPelletServerClient(
    internal val trackedSocket: NIOSocket,
    private val pool: PelletBufferPooling,
    override val codec: Codec
) : PelletServerClient {

    private val logger = pelletLogger<NIOPelletServerClient>()

    override val remoteHostString: String
        get() {
            return when (val remoteAddress = trackedSocket.channel.remoteAddress) {
                is InetSocketAddress -> remoteAddress.hostString
                is UnixDomainSocketAddress -> remoteAddress.path.toString()
                else -> remoteAddress.toString()
            }
        }

    override fun writeAndRelease(vararg buffer: PelletBuffer): Result<Long> {
        val byteCount = buffer.sumOf { it.remaining().toLong() }
        val buffers = buffer.map { it.byteBuffer }.toTypedArray()
        while (buffers.any { it.hasRemaining() }) {
            val attempt = runCatching {
                trackedSocket.channel.write(buffers)
            }
            if (attempt.isFailure) {
                return attempt
            }
        }
        buffer.forEach {
            pool.release(it)
        }
        return Result.success(byteCount)
    }

    override fun writeAndRelease(buffer: PelletBuffer): Result<Int> {
        val byteCount = buffer.byteBuffer.remaining()
        while (buffer.byteBuffer.hasRemaining()) {
            val attempt = runCatching {
                trackedSocket.channel.write(buffer.byteBuffer)
            }
            if (attempt.isFailure) {
                return attempt
            }
        }
        pool.release(buffer)
        return Result.success(byteCount)
    }

    override fun read(buffer: PelletBuffer): Result<Int> {
        return runCatching {
            trackedSocket.channel.read(buffer.byteBuffer)
        }
    }

    override fun close(source: CloseReason): Result<Unit> {
        logger.debug { "closed ${trackedSocket.channel} (initiator: $source)" }
        return runCatching {
            trackedSocket.channel.close()
        }
    }
}
