package dev.pellet.buffer

interface PelletBufferPooling {

    fun provide(): PelletBuffer
    fun release(buffer: PelletBuffer)
}
