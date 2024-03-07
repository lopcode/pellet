package dev.pellet.server.codec.http

import kotlinx.io.Buffer
import kotlinx.io.write
import java.nio.ByteBuffer

sealed class HTTPEntity {

    open val sizeBytes: Long = 0

    data object NoContent : HTTPEntity()
    data class Content(
        val buffer: Buffer
    ) : HTTPEntity() {

        override val sizeBytes = buffer.size

        constructor(byteBuffer: ByteBuffer) : this(
            Buffer().apply {
                this.write(byteBuffer)
            }
        )
    }
}
