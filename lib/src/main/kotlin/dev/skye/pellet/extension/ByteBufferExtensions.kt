package dev.skye.pellet.extension

import java.nio.ByteBuffer
import java.nio.charset.Charset

internal fun ByteBuffer.trimTrailing(byte: Byte): ByteBuffer {
    val limit = this.limit()

    if (limit < 1) {
        return this
    }
    if (this[limit - 1] == byte) {
        return this.limit(limit - 1)
    }

    return this
}

internal fun ByteBuffer.stringifyAndClear(charset: Charset = Charsets.US_ASCII): String {
    assert(this.position() == 0)
    val string = if (this.hasArray()) {
        String(this.array(), 0, this.limit(), charset)
    } else {
        val byteArray = ByteArray(this.limit())
        this.get(byteArray, 0, this.limit())
        String(byteArray, 0, this.limit(), charset)
    }
    this.clear()
    return string
}

// Returns the next position of a byte, by ignoring bytes before the current position()
internal fun ByteBuffer.nextPositionOfOrNull(needle: Byte): Int? {
    val position = this.position()
    val limit = this.limit()
    for (i in position.until(limit)) {
        val storedByte = this.get(i)
        if (needle == storedByte) {
            return i
        }
    }
    return null
}

// Advances a ByteBuffer position by count, up to and including the current limit
internal fun ByteBuffer.advance(count: Int): ByteBuffer {
    val newPosition = this.position() + count
    return if (newPosition > this.limit()) {
        this.position(this.limit())
    } else {
        this.position(newPosition)
    }
}
