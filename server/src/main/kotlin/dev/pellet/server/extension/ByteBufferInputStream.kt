package dev.pellet.server.extension

import java.io.InputStream
import java.nio.ByteBuffer

/**
 * Provides a streaming wrapper around a [ByteBuffer]
 */
data class ByteBufferInputStream(
    val byteBuffer: ByteBuffer
) : InputStream() {

    override fun read(): Int {
        if (!byteBuffer.hasRemaining()) {
            return -1
        }

        return byteBuffer.get().toInt()
    }

    override fun read(
        bytes: ByteArray?,
        offset: Int,
        length: Int
    ): Int {
        if (bytes == null) {
            return -1
        }
        if (length == 0) {
            return 0
        }
        val count = byteBuffer.remaining().coerceAtMost(length)
        if (count == 0) {
            return -1
        }
        byteBuffer.get(bytes, offset, count)
        return count
    }

    override fun readAllBytes(): ByteArray {
        return byteBuffer.array()
    }
}
