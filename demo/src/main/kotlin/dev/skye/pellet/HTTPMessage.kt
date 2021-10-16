package dev.skye.pellet

import java.nio.ByteBuffer

data class HTTPRequestMessage(
    val requestLine: HTTPRequestLine,
    val headers: HTTPHeaders,
    val entity: HTTPEntity
)

sealed class HTTPEntity {

    object NoContent : HTTPEntity()
    data class Content(val buffer: ByteBuffer) : HTTPEntity()
}

data class HTTPResponseMessage(
    val statusLine: HTTPStatusLine,
    val headers: HTTPHeaders,
    val entity: HTTPEntity
)

data class HTTPRequestLine(
    val method: String,
    val resourceUri: String,
    val httpVersion: String
)

data class HTTPStatusLine(
    val version: String,
    val statusCode: Int,
    val reasonPhrase: String
)
