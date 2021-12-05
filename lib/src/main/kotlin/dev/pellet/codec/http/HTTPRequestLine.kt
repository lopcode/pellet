package dev.pellet.codec.http

data class HTTPRequestLine(
    val method: HTTPMethod,
    val resourceUri: String,
    val httpVersion: String
)
