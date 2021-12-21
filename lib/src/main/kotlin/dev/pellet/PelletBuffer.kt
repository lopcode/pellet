package dev.pellet

import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

data class PelletBuffer(
    val byteBuffer: ByteBuffer,
    val used: AtomicBoolean
) {

    fun put(byte: Byte): PelletBuffer {
        this.byteBuffer.put(byte)
        return this
    }

    fun put(array: ByteArray): PelletBuffer {
        this.byteBuffer.put(array)
        return this
    }

    fun put(buffer: ByteBuffer): PelletBuffer {
        this.byteBuffer.put(buffer)
        return this
    }

    fun remaining(): Int {
        return this.byteBuffer.remaining()
    }

    fun flip(): PelletBuffer {
        this.byteBuffer.flip()
        return this
    }
}
