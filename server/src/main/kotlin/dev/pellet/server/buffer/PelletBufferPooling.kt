package dev.pellet.server.buffer

interface PelletBufferPooling {

    fun provide(): PelletBuffer
    fun release(buffer: PelletBuffer)
}
