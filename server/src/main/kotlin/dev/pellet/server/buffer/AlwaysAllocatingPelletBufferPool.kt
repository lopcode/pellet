package dev.pellet.server.buffer

import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

class AlwaysAllocatingPelletBufferPool(
    private val allocationSize: Int
) : PelletBufferPooling {

    override fun provide(): PelletBuffer {
        val buffer = ByteBuffer.allocate(allocationSize)
        return PelletBuffer(
            buffer,
            used = AtomicBoolean(true)
        )
    }

    override fun release(buffer: PelletBuffer) {
        buffer.used.set(false)
    }
}
