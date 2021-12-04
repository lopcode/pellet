package dev.pellet.codec.http

data class HTTPRequestLine(
    val method: String,
    val resourceUri: String,
    val httpVersion: String
)
