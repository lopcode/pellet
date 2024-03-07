package dev.pellet.server.codec.http

import dev.pellet.logging.pelletLogger
import dev.pellet.server.PelletServerClient
import dev.pellet.server.codec.Codec
import dev.pellet.server.extension.indexOfOrNull
import dev.pellet.server.extension.stringifyAndClear
import dev.pellet.server.extension.trimLWS
import kotlinx.io.Buffer
import java.net.URI
import java.util.Locale
import kotlin.math.min

internal class HTTPMessageCodec(
    private val processor: (HTTPRequestMessage, PelletServerClient) -> Unit
) : Codec {

    private val requestLineBuffer = Buffer()
    private val headersBuffer = Buffer()

    private var expectedEntityOctets: Long = -1
    private var readEntityOctets: Long = -1
    private val entityBuffer = Buffer()
    private val chunkLineBuffer = Buffer()
    private var expectedChunkSizeOctets: Long = -1
    private var readChunkSizeOctets: Long = -1
    private var entity: HTTPEntity? = null

    private var chunkState = ChunkConsumeState.SIZE_LINE

    private var requestLine: HTTPRequestLine? = null
    private var headers = HTTPHeaders()

    companion object {

        private const val MAX_FIXED_ENTITY_OCTETS = 16 * 1024
    }

    private enum class ConsumeState {
        REQUEST_LINE,
        HEADERS,
        FIXED_ENTITY,
        CHUNKED_ENTITY
    }

    private enum class ChunkConsumeState {
        SIZE_LINE,
        DATA_LINE,
        END_LINE
    }

    private var state = ConsumeState.REQUEST_LINE
    private val logger = pelletLogger<HTTPMessageCodec>()

    override fun clear() {
        requestLineBuffer.clear()
        headersBuffer.clear()
        entityBuffer.clear()
        chunkLineBuffer.clear()
        expectedChunkSizeOctets = -1
        readChunkSizeOctets = -1
        expectedEntityOctets = -1
        readEntityOctets = -1
        state = ConsumeState.REQUEST_LINE
        chunkState = ChunkConsumeState.SIZE_LINE
        requestLine = null
        entity = null
        headers = HTTPHeaders()
    }

    override fun consume(
        buffer: Buffer,
        client: PelletServerClient
    ) {
        readLoop@ while (!buffer.exhausted()) {
            when (state) {
                ConsumeState.REQUEST_LINE -> {
                    if (!consumeLine(buffer, requestLineBuffer)) {
                        continue@readLoop
                    }

                    handleRequestLine()
                }
                ConsumeState.HEADERS -> {
                    if (!consumeLine(buffer, headersBuffer)) {
                        continue@readLoop
                    }

                    handleHeaderLine()
                    if (state == ConsumeState.REQUEST_LINE) {
                        processor(buildMessage(), client)
                    }
                }
                ConsumeState.FIXED_ENTITY -> {
                    if (!consumeFixedEntity(buffer, entityBuffer)) {
                        continue@readLoop
                    }

                    handleFixedEntity()
                    processor(buildMessage(), client)
                }
                ConsumeState.CHUNKED_ENTITY -> {
                    when (chunkState) {
                        ChunkConsumeState.SIZE_LINE -> {
                            if (!consumeLine(buffer, chunkLineBuffer)) {
                                continue@readLoop
                            }

                            handleChunkSizeLine()
                        }
                        ChunkConsumeState.DATA_LINE -> {
                            if (!consumeDataChunk(buffer, entityBuffer)) {
                                continue@readLoop
                            }

                            chunkState = ChunkConsumeState.SIZE_LINE
                        }
                        ChunkConsumeState.END_LINE -> {
                            if (!consumeLine(buffer, chunkLineBuffer)) {
                                continue@readLoop
                            }

                            val endLine = chunkLineBuffer.stringifyAndClear(Charsets.US_ASCII)
                            assert(endLine.isEmpty())

                            logger.debug { "read chunk end line" }
                            handleChunkedEntity()
                            processor(buildMessage(), client)
                        }
                    }
                }
            }
        }
    }

    private fun handleChunkSizeLine() {
        val expectedChunkSizeOctets = chunkLineBuffer
            .stringifyAndClear(Charsets.US_ASCII)
            .toLong(radix = 16)
        assert(expectedChunkSizeOctets >= 0)
        assert((expectedChunkSizeOctets + readChunkSizeOctets) < MAX_FIXED_ENTITY_OCTETS)
        when (expectedChunkSizeOctets) {
            0L -> {
                chunkState = ChunkConsumeState.END_LINE
            }
            else -> {
                this.expectedChunkSizeOctets = expectedChunkSizeOctets
                this.readChunkSizeOctets = 0
                chunkState = ChunkConsumeState.DATA_LINE
            }
        }
        chunkLineBuffer.clear()
    }

    private fun consumeFixedEntity(source: Buffer, target: Buffer): Boolean {
        if (readEntityOctets < expectedEntityOctets) {
            val remainingOctets = expectedEntityOctets - readEntityOctets
            val availableOctets = min(source.size, remainingOctets)
            target.write(source, availableOctets)
            readEntityOctets += availableOctets
        }

        if (readEntityOctets < expectedEntityOctets) {
            return false
        }

        return true
    }

    private fun consumeDataChunk(source: Buffer, target: Buffer): Boolean {
        if (readChunkSizeOctets < expectedChunkSizeOctets) {
            val remainingOctets = expectedChunkSizeOctets - readChunkSizeOctets
            val availableOctets = min(source.size, remainingOctets)
            target.write(source, availableOctets)
            readChunkSizeOctets += availableOctets
        }

        val expectedSizeWithTrailingCRLF = expectedChunkSizeOctets + 2
        if (!source.exhausted() && readChunkSizeOctets < expectedSizeWithTrailingCRLF) {
            val remainingOctets = expectedSizeWithTrailingCRLF - readChunkSizeOctets
            val availableOctets = min(source.size, remainingOctets)
            source.skip(availableOctets)
            readChunkSizeOctets += availableOctets
        }

        if (readChunkSizeOctets < expectedSizeWithTrailingCRLF) {
            return false
        }

        return true
    }

    private fun handleFixedEntity() {
        entity = HTTPEntity.Content(
            buffer = entityBuffer.copy()
        )
        entityBuffer.clear()
        expectedEntityOctets = -1
        readEntityOctets = -1
        state = ConsumeState.REQUEST_LINE
    }

    private fun handleChunkedEntity() {
        entity = HTTPEntity.Content(
            buffer = entityBuffer.copy()
        )
        entityBuffer.clear()
        expectedChunkSizeOctets = -1
        readChunkSizeOctets = -1
        state = ConsumeState.REQUEST_LINE
    }

    private fun handleRequestLine() {
        val requestLineString = requestLineBuffer.stringifyAndClear(Charsets.US_ASCII)
        val parsedRequestLine = parseRequestLine(requestLineString).getOrThrow()
        logger.debug { "got request line: $parsedRequestLine" }
        this.requestLine = parsedRequestLine
        state = ConsumeState.HEADERS
    }

    private fun handleHeaderLine() {
        val headerLine = headersBuffer.stringifyAndClear(Charsets.US_ASCII)
        when {
            headerLine.isNotEmpty() -> {
                val parsedHeader = parseHeaderLine(headerLine).getOrNull()
                if (parsedHeader != null) {
                    headers.add(parsedHeader)
                }
            }
            else -> handleEndOfHeaders()
        }
    }

    private fun handleEndOfHeaders() {
        logger.debug { "got headers: $headers" }

        val transferEncoding = headers.getSingleOrNull(HTTPHeaderConstants.transferEncoding)
        val isChunked = transferEncoding?.rawValue?.equals(HTTPHeaderConstants.chunked) ?: false
        val contentLength = headers.getSingleOrNull(HTTPHeaderConstants.contentLength)?.rawValue

        val requestLine = this.requestLine ?: throw RuntimeException("no request line available")
        state = when {
            requestLine.method == HTTPMethod.Head -> {
                ConsumeState.REQUEST_LINE
            }
            isChunked -> {
                chunkState = ChunkConsumeState.SIZE_LINE
                ConsumeState.CHUNKED_ENTITY
            }
            contentLength != null -> {
                this.expectedEntityOctets = contentLength.toLongOrNull()
                    ?: throw RuntimeException("failed to parse content length")
                // todo: make configurable
                if (this.expectedEntityOctets < 0 || this.expectedEntityOctets > MAX_FIXED_ENTITY_OCTETS) {
                    throw RuntimeException("content length out of acceptable bounds")
                }

                if (expectedEntityOctets == 0L) {
                    this.entity = HTTPEntity.NoContent
                    state = ConsumeState.REQUEST_LINE
                    return
                }

                this.readEntityOctets = 0
                ConsumeState.FIXED_ENTITY
            }
            else -> {
                ConsumeState.REQUEST_LINE
            }
        }
        logger.debug { "state after headers: $state" }
    }

    // Copies bytes from the source to the target buffer until LF is found
    // Returns true if a CRLF terminated line as been accumulated, false otherwise
    private fun consumeLine(source: Buffer, target: Buffer): Boolean {
        require(!source.exhausted())
        when (val nextLineFeedPosition = source.indexOfOrNull(HTTPCharacters.LINE_FEED_BYTE)) {
            null -> {
                // No LF found in buffer - accumulate the entirety of source
                source.transferTo(target)
                return false
            }
            0L -> {
                // LF at the start of the source buffer
                // Avoid a "zero copy" by skipping over it
                source.skip(1)
            }
            else -> {
                // LF found somewhere in the buffer - manually copy bytes preceding it
                val lineEndLength = if (source[nextLineFeedPosition - 1] == HTTPCharacters.CARRIAGE_RETURN_BYTE) {
                    2L
                } else {
                    1L
                }
                target.write(source, nextLineFeedPosition - lineEndLength + 1)
                source.skip(lineEndLength)
            }
        }

        return true
    }

    private fun buildMessage(): HTTPRequestMessage {
        val requestLine = this.requestLine
            ?: throw RuntimeException("tried to output message without a request line")

        val entity = this.entity ?: HTTPEntity.NoContent
        val message = HTTPRequestMessage(
            requestLine = requestLine,
            headers = headers.copy(),
            entity = entity
        )

        clear()
        return message
    }

    private fun parseRequestLine(line: String): Result<HTTPRequestLine> {
        /*
        The Request-Line begins with a method token, followed by the
        Request-URI and the protocol version, and ending with CRLF. The
        elements are separated by SP characters. No CR or LF is allowed
        except in the final CRLF sequence.

        Request-Line   = Method SP Request-URI SP HTTP-Version CRLF

       Method         = "OPTIONS"                ; Section 9.2
                      | "GET"                    ; Section 9.3
                      | "HEAD"                   ; Section 9.4
                      | "POST"                   ; Section 9.5
                      | "PUT"                    ; Section 9.6
                      | "DELETE"                 ; Section 9.7
                      | "TRACE"                  ; Section 9.8
                      | "CONNECT"                ; Section 9.9
                      | extension-method
       extension-method = token
        */

        val methodSpaceIndex = line.indexOf(HTTPCharacters.SPACE)
        if (methodSpaceIndex <= 0 || methodSpaceIndex + 1 > line.length) {
            return Result.failure(RuntimeException("expected request line to have 3 parts"))
        }
        val resourceURISpaceIndex = line.indexOf(HTTPCharacters.SPACE, methodSpaceIndex + 1)
        if (resourceURISpaceIndex <= 0 || resourceURISpaceIndex + 1 > line.length) {
            return Result.failure(RuntimeException("expected request line to have 3 parts"))
        }
        val method = line.substring(0, methodSpaceIndex)
        val rawResourceURI = line.substring(methodSpaceIndex + 1, resourceURISpaceIndex)
        val httpVersion = line.substring(resourceURISpaceIndex + 1)

        // todo: verify method is a "token" entity
        if (method.isEmpty()) {
            return Result.failure(RuntimeException("malformed method"))
        }

        // todo: verify uri is ""*" | absoluteURI | abs_path | authority"
        if (rawResourceURI.isEmpty()) {
            return Result.failure(RuntimeException("malformed raw uri"))
        }

        val resourceURI = try {
            URI.create(rawResourceURI)
        } catch (exception: IllegalArgumentException) {
            return Result.failure(RuntimeException("malformed uri"))
        }

        // todo: verify version is ""HTTP" "/" 1*DIGIT "." 1*DIGIT"
        if (httpVersion.isEmpty()) {
            return Result.failure(RuntimeException("malformed version"))
        }

        val httpMethod = defaultMethods.firstOrNull {
            it.rawMethod == method.uppercase(Locale.ENGLISH)
        } ?: HTTPMethod.Custom(method)

        return Result.success(
            HTTPRequestLine(
                method = httpMethod,
                resourceUri = resourceURI,
                httpVersion = httpVersion
            )
        )
    }

    private fun parseHeaderLine(line: String): Result<HTTPHeader> {
        /*
           message-header = field-name ":" [ field-value ]
           field-name     = token
           field-value    = *( field-content | LWS )
           field-content  = <the OCTETs making up the field-value
                            and consisting of either *TEXT or combinations
                            of token, separators, and quoted-string>
         */
        val firstColonIndex = line.indexOf(HTTPCharacters.COLON)
        if (firstColonIndex <= 0) {
            return Result.failure(RuntimeException("malformed header - missing colon or name"))
        }

        val valueStartIndex = firstColonIndex + 1
        if (valueStartIndex >= line.length) {
            return Result.failure(RuntimeException("malformed header - missing value"))
        }

        val name = line.substring(0, firstColonIndex)
        // todo: value is actually just opaque octets that can be treated as text in context - not necessarily
        //  us-ascii?
        val value = line
            .substring(valueStartIndex)
            .trimLWS()

        return Result.success(
            HTTPHeader(
                rawName = name,
                rawValue = value
            )
        )
    }
}
