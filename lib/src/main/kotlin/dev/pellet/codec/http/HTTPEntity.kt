package dev.pellet.codec.http

import dev.pellet.buffer.PelletBuffer

sealed class HTTPEntity {

    object NoContent : HTTPEntity()
    data class Content(
        val buffer: PelletBuffer
    ) : HTTPEntity()
}
