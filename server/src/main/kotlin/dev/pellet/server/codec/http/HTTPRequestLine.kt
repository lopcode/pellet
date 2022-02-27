package dev.pellet.server.codec.http

import java.net.URI

data class HTTPRequestLine(
    val method: HTTPMethod,
    val resourceUri: URI,
    val httpVersion: String
)
