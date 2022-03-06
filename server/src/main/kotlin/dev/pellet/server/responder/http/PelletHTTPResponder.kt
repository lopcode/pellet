package dev.pellet.server.responder.http

import dev.pellet.logging.pelletLogger
import dev.pellet.server.PelletServerClient
import dev.pellet.server.buffer.PelletBuffer
import dev.pellet.server.buffer.PelletBufferPooling
import dev.pellet.server.codec.http.HTTPCharacters
import dev.pellet.server.codec.http.HTTPEntity
import dev.pellet.server.codec.http.HTTPHeader
import dev.pellet.server.codec.http.HTTPHeaderConstants
import dev.pellet.server.codec.http.HTTPHeaders
import dev.pellet.server.codec.http.HTTPResponseMessage
import dev.pellet.server.codec.http.HTTPStatusLine

class PelletHTTPResponder(
    private val client: PelletServerClient,
    private val pool: PelletBufferPooling
) : PelletHTTPResponding {

    private val logger = pelletLogger<PelletHTTPResponder>()

    override suspend fun respond(message: HTTPResponseMessage): Result<Unit> {
        val effectiveResponse = buildEffectiveResponse(message)
        return client
            .respond(effectiveResponse, pool)
            .map { }
    }
}

private suspend fun PelletServerClient.respond(
    message: HTTPResponseMessage,
    pool: PelletBufferPooling
): Result<Int> {
    val buffer = pool.provide()
        .appendStatusLine(message.statusLine)
        .appendHeaders(message.headers)
        .appendEntity(message.entity) // todo: good place for streaming/chunked entity logic
        .flip()
    return this.writeAndRelease(buffer)
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
        // todo: what happens when different values have different cased keys?
        // for now - choose the first one
        val headerName = values.firstOrNull()?.rawName ?: key
        this.put(headerName.toByteArray(Charsets.US_ASCII))
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
