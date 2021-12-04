package dev.pellet.codec.http

data class HTTPRequestMessage(
    val requestLine: HTTPRequestLine,
    val headers: HTTPHeaders,
    val entity: HTTPEntity
)
