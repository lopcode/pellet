package dev.pellet.server.codec.http

import dev.pellet.server.buffer.PelletBuffer
import java.nio.ByteBuffer
import java.nio.charset.Charset

sealed class HTTPEntity {

    open val sizeBytes: Int = 0

    object NoContent : HTTPEntity()
    data class Content(
        val buffer: PelletBuffer,
        val contentType: String?
    ) : HTTPEntity() {

        companion object {

            fun of(
                byteBuffer: ByteBuffer,
                contentType: String
            ): Content {
                val buffer = PelletBuffer(byteBuffer)
                return Content(buffer, contentType)
            }

            fun of(
                string: String,
                charset: Charset = Charsets.UTF_8,
                contentType: String // todo: set charset when content type is modeled
            ): Content {
                val bytes = charset.encode(string)
                return of(bytes, contentType)
            }
        }

        override val sizeBytes = buffer.limit()
    }
}
