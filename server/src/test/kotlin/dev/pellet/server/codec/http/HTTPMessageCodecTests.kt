package dev.pellet.server.codec.http

import dev.pellet.server.buffer.PelletBuffer
import dev.pellet.server.buffer.PelletBufferPooling
import java.net.URI

private fun PelletBufferPooling.bufferOf(string: String): PelletBuffer {
    val bytes = string.toByteArray(charset = Charsets.UTF_8)
    val buffer = this.provide()
    buffer.byteBuffer.put(bytes)
    buffer.byteBuffer.flip()
    return buffer
}

private fun buildMessage(
    method: HTTPMethod,
    resourceUri: String,
    headers: HTTPHeaders = HTTPHeaders(),
    entity: HTTPEntity = HTTPEntity.NoContent
): HTTPRequestMessage {
    return HTTPRequestMessage(
        requestLine = HTTPRequestLine(
            method = method,
            resourceUri = URI.create(resourceUri),
            httpVersion = "HTTP/1.1"
        ),
        headers = headers,
        entity = entity
    )
}
