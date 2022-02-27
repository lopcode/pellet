package dev.pellet.server.codec.http

import dev.pellet.server.buffer.PelletBuffer

sealed class HTTPEntity {

    object NoContent : HTTPEntity()
    data class Content(
        val buffer: PelletBuffer
    ) : HTTPEntity()
}
