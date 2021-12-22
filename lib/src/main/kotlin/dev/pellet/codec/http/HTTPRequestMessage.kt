package dev.pellet.codec.http

import dev.pellet.buffer.PelletBufferPooling

data class HTTPRequestMessage(
    val requestLine: HTTPRequestLine,
    val headers: HTTPHeaders,
    val entity: HTTPEntity
) {

    internal fun release(
        pool: PelletBufferPooling
    ) {
        when (this.entity) {
            is HTTPEntity.Content -> {
                pool.release(this.entity.buffer)
            }
            else -> {}
        }
    }
}
