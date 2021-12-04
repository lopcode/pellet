package dev.pellet.codec.http

data class HTTPResponseMessage(
    val statusLine: HTTPStatusLine,
    val headers: HTTPHeaders,
    val entity: HTTPEntity
)
