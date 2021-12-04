package dev.pellet.codec.http

import java.nio.ByteBuffer

sealed class HTTPEntity {

    object NoContent : HTTPEntity()
    data class Content(val buffer: ByteBuffer) : HTTPEntity()
}
