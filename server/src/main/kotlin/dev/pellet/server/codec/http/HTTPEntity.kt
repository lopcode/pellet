package dev.pellet.server.codec.http

import dev.pellet.server.buffer.PelletBuffer

sealed class HTTPEntity {

    open val sizeBytes: Int = 0

    object NoContent : HTTPEntity()
    data class Content(
        val buffer: PelletBuffer
    ) : HTTPEntity() {

        override val sizeBytes = buffer.limit()
    }
}
