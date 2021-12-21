package dev.pellet

import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

class PelletBufferPool(
    private val allocationSize: Int
) {

    private val buffers = ArrayDeque<PelletBuffer>()

    init {
        repeat(100) {
            allocateAndTrackNewBuffer(
                used = false
            )
        }
    }

    // todo: allocation pressure, releasing heuristics
    fun provide(): PelletBuffer = synchronized(buffers) {
        val maybeBuffer = buffers.firstOrNull {
            !it.used.get()
        }

        return when (maybeBuffer) {
            null -> {
                allocateAndTrackNewBuffer(
                    used = true
                )
            }
            else -> {
                maybeBuffer.byteBuffer.clear()
                maybeBuffer.used.set(true)
                maybeBuffer
            }
        }
    }

    fun release(buffer: PelletBuffer) {
        buffer.used.set(false)
    }

    private fun allocateAndTrackNewBuffer(
        used: Boolean
    ): PelletBuffer {
        val backingBuffer = ByteBuffer
            .allocate(allocationSize)
            .clear()
        val newBuffer = PelletBuffer(
            backingBuffer,
            AtomicBoolean(used)
        )
        if (used) {
            buffers.addLast(newBuffer)
        } else {
            buffers.addFirst(newBuffer)
        }
        return newBuffer
    }
}
