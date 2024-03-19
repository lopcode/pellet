package dev.pellet.server.responder.http

import dev.pellet.logging.pelletLogger
import dev.pellet.server.PelletServerClient
import dev.pellet.server.codec.http.HTTPCharacters
import dev.pellet.server.codec.http.HTTPEntity
import dev.pellet.server.codec.http.HTTPHeader
import dev.pellet.server.codec.http.HTTPHeaderConstants
import dev.pellet.server.codec.http.HTTPHeaders
import dev.pellet.server.codec.http.HTTPResponseMessage
import dev.pellet.server.codec.http.HTTPStatusLine
import kotlinx.io.Buffer
import kotlinx.io.bytestring.ByteString

internal class PelletHTTPResponder(
    private val client: PelletServerClient
) : PelletHTTPResponding {

    private val logger = pelletLogger<PelletHTTPResponder>()

    override fun respond(message: HTTPResponseMessage): Result<Unit> {
        val effectiveResponse = buildEffectiveResponse(message)
        return writeResponse(
            effectiveResponse,
            client
        ).map { }
    }
}

private fun writeResponse(
    message: HTTPResponseMessage,
    client: PelletServerClient
): Result<Unit> {
    ByteString()
    val nonEntityBuffer = Buffer()
        .appendStatusLine(message.statusLine)
        .appendHeaders(message.headers)
    if (message.entity !is HTTPEntity.Content) {
        return client
            .write(nonEntityBuffer)
            .map {}
    }
    return client
        .write(nonEntityBuffer, message.entity.buffer)
        .map {}
}

private fun Buffer.appendStatusLine(
    statusLine: HTTPStatusLine
): Buffer {
    return this
        .put(statusLine.version.toByteArray(Charsets.US_ASCII))
        .put(HTTPCharacters.SPACE_BYTE)
        .put(statusLine.statusCode.toString(10).toByteArray(Charsets.US_ASCII))
        .put(HTTPCharacters.SPACE_BYTE)
        .put(statusLine.reasonPhrase.toByteArray(Charsets.US_ASCII))
        .putCRLF()
}

private fun Buffer.appendHeaders(
    headers: HTTPHeaders
): Buffer {
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

private fun Buffer.putCRLF(): Buffer {
    return this
        .put(HTTPCharacters.CARRIAGE_RETURN_BYTE)
        .put(HTTPCharacters.LINE_FEED_BYTE)
}

private fun buildEffectiveResponse(
    original: HTTPResponseMessage
): HTTPResponseMessage {
    if (original.entity is HTTPEntity.Content) {
        val headers = original.headers
            .add(
                HTTPHeader(
                    HTTPHeaderConstants.contentLength,
                    original.entity.buffer.size.toString(10)
                )
            )
        val effectiveResponse = original.copy(
            headers = headers
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

fun Buffer.put(byte: Byte): Buffer {
    this.writeByte(byte)
    return this
}

fun Buffer.put(byteArray: ByteArray): Buffer {
    this.write(byteArray)
    return this
}
