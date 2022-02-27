package dev.pellet.server.buffer

import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A placeholder buffer implementation which uses a regular ByteBuffer
 * Intended to:
 * * Make it easier to see the required API surface of a buffer implementation
 * * Make implementation of zero-copy operations possible in the future
 *
 * The [used] variable is present just to test what a manual-release / pooling
 * api looks like
 */
data class PelletBuffer(
    val byteBuffer: ByteBuffer,
    internal val used: AtomicBoolean
) {

    fun put(
        byte: Byte
    ): PelletBuffer {
        this.byteBuffer.put(byte)
        return this
    }

    fun put(
        array: ByteArray
    ): PelletBuffer {
        this.byteBuffer.put(array)
        return this
    }

    fun put(
        buffer: PelletBuffer
    ): PelletBuffer {
        this.byteBuffer.put(buffer.byteBuffer)
        return this
    }

    fun put(
        targetPosition: Int,
        source: PelletBuffer,
        sourcePosition: Int,
        length: Int
    ): PelletBuffer {
        this.byteBuffer.put(
            targetPosition,
            source.byteBuffer,
            sourcePosition,
            length
        )
        return this
    }

    fun hasRemaining(): Boolean {
        return this.byteBuffer.hasRemaining()
    }

    fun capacity(): Int {
        return this.byteBuffer.capacity()
    }

    fun remaining(): Int {
        return this.byteBuffer.remaining()
    }

    fun limit(): Int {
        return this.byteBuffer.limit()
    }

    fun limit(
        index: Int
    ): PelletBuffer {
        this.byteBuffer.limit(index)
        return this
    }

    fun position(): Int {
        return this.byteBuffer.position()
    }

    fun position(
        index: Int
    ): PelletBuffer {
        this.byteBuffer.position(index)
        return this
    }

    fun hasArray(): Boolean {
        return this.byteBuffer.hasArray()
    }

    fun array(): ByteArray {
        return this.byteBuffer.array()
    }

    fun get(
        destination: ByteArray,
        offset: Int,
        length: Int
    ): PelletBuffer {
        this.byteBuffer.get(destination, offset, length)
        return this
    }

    fun clear(): PelletBuffer {
        this.byteBuffer.clear()
        return this
    }

    operator fun get(
        index: Int
    ): Byte {
        return this.byteBuffer.get(index)
    }

    fun flip(): PelletBuffer {
        this.byteBuffer.flip()
        return this
    }
}
