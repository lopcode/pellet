package dev.pellet.responder.http

import dev.pellet.PelletBuffer
import dev.pellet.PelletBufferPool
import dev.pellet.PelletClient
import dev.pellet.codec.http.HTTPCharacters
import dev.pellet.codec.http.HTTPEntity
import dev.pellet.codec.http.HTTPHeader
import dev.pellet.codec.http.HTTPHeaderConstants
import dev.pellet.codec.http.HTTPHeaders
import dev.pellet.codec.http.HTTPResponseMessage
import dev.pellet.codec.http.HTTPStatusLine

class PelletHTTPResponder(
    private val client: PelletClient,
    private val pool: PelletBufferPool
) {

    suspend fun writeNoContent() {
        val message = HTTPResponseMessage(
            statusLine = HTTPStatusLine(
                version = "HTTP/1.1",
                statusCode = 204,
                reasonPhrase = "No Content"
            ),
            headers = HTTPHeaders(),
            entity = HTTPEntity.NoContent
        )
        val effectiveResponse = buildEffectiveResponse(message)

        client.write(effectiveResponse, pool)
    }

    suspend fun writeNotFound() {
        val message = HTTPResponseMessage(
            statusLine = HTTPStatusLine(
                version = "HTTP/1.1",
                statusCode = 404,
                reasonPhrase = "Not Found"
            ),
            headers = HTTPHeaders(),
            entity = HTTPEntity.NoContent
        )
        val effectiveResponse = buildEffectiveResponse(message)
        client.write(effectiveResponse, pool)
    }

    private fun buildEffectiveResponse(
        original: HTTPResponseMessage
    ): HTTPResponseMessage {
        if (original.entity is HTTPEntity.Content) {
            // todo: add content type
            val effectiveResponse = original.copy(
                headers = original.headers.add(
                    HTTPHeader(
                        HTTPHeaderConstants.contentLength,
                        original.entity.buffer.limit().toString(10)
                    )
                )
            )
            return effectiveResponse
        }

        if (original.entity is HTTPEntity.NoContent && original.statusLine.statusCode != 204) {
            val effectiveResponse = original.copy(
                headers = original.headers.add(
                    HTTPHeader(
                        HTTPHeaderConstants.contentLength,
                        "0"
                    )
                )
            )
            return effectiveResponse
        }

        return original
    }
}

private suspend fun PelletClient.write(
    message: HTTPResponseMessage,
    pool: PelletBufferPool
) {
    val buffer = pool.provide()
        .appendStatusLine(message.statusLine)
        .appendHeaders(message.headers)
        .appendEntity(message.entity)
        .flip()
    this.write(buffer)
}

private fun PelletBuffer.appendStatusLine(
    statusLine: HTTPStatusLine
): PelletBuffer {
    return this
        .put(statusLine.version.toByteArray(Charsets.US_ASCII))
        .put(HTTPCharacters.SPACE_BYTE)
        .put(statusLine.statusCode.toString(10).toByteArray(Charsets.US_ASCII))
        .put(HTTPCharacters.SPACE_BYTE)
        .put(statusLine.reasonPhrase.toByteArray(Charsets.US_ASCII))
        .putCRLF()
}

private fun PelletBuffer.appendHeaders(
    headers: HTTPHeaders
): PelletBuffer {
    headers.forEach { key, values ->
        this.put(key.toByteArray(Charsets.US_ASCII))
            .put(HTTPCharacters.COLON_BYTE)
            .put(HTTPCharacters.SPACE_BYTE)
            // todo: figure out value encoding
            .put(values.joinToString(HTTPCharacters.COMMA.toString()) { it.rawValue }.toByteArray(Charsets.US_ASCII))
            .putCRLF()
    }
    return this.putCRLF()
}

private fun PelletBuffer.appendEntity(
    entity: HTTPEntity
): PelletBuffer {
    when (entity) {
        is HTTPEntity.NoContent -> {}
        is HTTPEntity.Content -> {
            this.put(entity.buffer)
        }
    }

    return this
}

private fun PelletBuffer.putCRLF(): PelletBuffer {
    return this
        .put(HTTPCharacters.CARRIAGE_RETURN_BYTE)
        .put(HTTPCharacters.LINE_FEED_BYTE)
}
