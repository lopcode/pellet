package dev.pellet.server.codec.http

data class HTTPRequestMessage(
    val requestLine: HTTPRequestLine,
    val headers: HTTPHeaders,
    val entity: HTTPEntity
)
