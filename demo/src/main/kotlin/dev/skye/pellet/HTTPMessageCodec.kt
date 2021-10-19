package dev.skye.pellet

import java.nio.ByteBuffer
import kotlin.math.min

class HTTPMessageCodec {

    private val requestLineBuffer = ByteBuffer.allocate(4096)
    private val headersBuffer = ByteBuffer.allocate(4096)

    private var expectedEntityOctets: Int = -1
    private var readEntityOctets: Int = -1
    private val entityBuffer = ByteBuffer.allocate(1024 * 16)
    private val chunkLineBuffer = ByteBuffer.allocate(256)
    private var expectedChunkSizeOctets: Int = -1
    private var readChunkSizeOctets: Int = -1
    private var entity: HTTPEntity? = null

    private var chunkState = ChunkConsumeState.SIZE_LINE

    private var requestLine: HTTPRequestLine? = null
    private var headers = HTTPHeaders()

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
    private val logger = logger<HTTPMessageCodec>()

    fun consume(bytes: ByteBuffer): List<HTTPRequestMessage> {
        val messages = mutableListOf<HTTPRequestMessage>()
        readLoop@ while (bytes.hasRemaining()) {
            when (state) {
                ConsumeState.REQUEST_LINE -> {
                    if (!consumeLine(bytes, requestLineBuffer)) {
                        continue@readLoop
                    }

                    handleRequestLine()
                }
                ConsumeState.HEADERS -> {
                    if (!consumeLine(bytes, headersBuffer)) {
                        continue@readLoop
                    }

                    handleHeaderLine(messages)
                }
                ConsumeState.FIXED_ENTITY -> {
                    if (!consumeFixedEntity(bytes, entityBuffer)) {
                        continue@readLoop
                    }

                    handleFixedEntity(messages)
                }
                ConsumeState.CHUNKED_ENTITY -> {
                    when (chunkState) {
                        ChunkConsumeState.SIZE_LINE -> {
                            if (!consumeLine(bytes, chunkLineBuffer)) {
                                continue@readLoop
                            }

                            handleChunkSizeLine()
                        }
                        ChunkConsumeState.DATA_LINE -> {
                            if (!consumeDataChunk(bytes, entityBuffer)) {
                                continue@readLoop
                            }

                            logger.info("read data chunk line")
                            chunkState = ChunkConsumeState.SIZE_LINE
                        }
                        ChunkConsumeState.END_LINE -> {
                            if (!consumeLine(bytes, chunkLineBuffer)) {
                                continue@readLoop
                            }

                            val endLine = chunkLineBuffer.stringifyAndClear(Charsets.US_ASCII)
                            assert(endLine.isEmpty())

                            logger.info("read chunk end line")
                            handleChunkedEntity(messages)
                        }
                    }
                }
            }
        }

        return messages
    }

    private fun handleChunkSizeLine() {
        val expectedChunkSizeOctets = chunkLineBuffer
            .stringifyAndClear(Charsets.US_ASCII)
            .toInt(radix = 16)
        assert(expectedChunkSizeOctets >= 0)
        assert((expectedChunkSizeOctets + readChunkSizeOctets) < entityBuffer.capacity())
        when (expectedChunkSizeOctets) {
            0 -> {
                logger.info("read last chunk size line")
                chunkState = ChunkConsumeState.END_LINE
            }
            else -> {
                this.expectedChunkSizeOctets = expectedChunkSizeOctets
                this.readChunkSizeOctets = 0
                chunkState = ChunkConsumeState.DATA_LINE
                logger.info("read size chunk line")
            }
        }
        chunkLineBuffer.clear()
    }

    private fun consumeFixedEntity(source: ByteBuffer, target: ByteBuffer): Boolean {
        if (readEntityOctets < expectedEntityOctets) {
            val remainingOctets = expectedEntityOctets - readEntityOctets
            val availableOctets = min(source.remaining(), remainingOctets)
            target.put(readEntityOctets, source, source.position(), availableOctets)
            readEntityOctets += availableOctets
            target.position(readEntityOctets)
            source.advance(availableOctets)
        }

        if (readEntityOctets < expectedEntityOctets) {
            return false
        }

        return true
    }

    private fun consumeDataChunk(source: ByteBuffer, target: ByteBuffer): Boolean {
        val expectedSizeWithTrailingCRLF = expectedChunkSizeOctets + 2
        if (readChunkSizeOctets < expectedSizeWithTrailingCRLF) {
            val remainingOctets = expectedSizeWithTrailingCRLF - readChunkSizeOctets
            val availableOctets = min(source.remaining(), remainingOctets)
            target.put(readChunkSizeOctets, source, source.position(), availableOctets)
            readChunkSizeOctets += availableOctets
            target.position(readChunkSizeOctets)
            source.advance(availableOctets)
        }

        if (readChunkSizeOctets < expectedSizeWithTrailingCRLF) {
            return false
        }

        target.trimTrailing(HTTPCharacters.LINE_FEED_BYTE)
        target.trimTrailing(HTTPCharacters.CARRIAGE_RETURN_BYTE)
        return true
    }

    private fun handleFixedEntity(messages: MutableList<HTTPRequestMessage>) {
        entityBuffer.flip()
        entity = HTTPEntity.Content(
            buffer = ByteBuffer
                .allocate(expectedEntityOctets)
                .put(entityBuffer)
                .flip()
                .asReadOnlyBuffer()
        )
        entityBuffer.clear()
        expectedEntityOctets = -1
        readEntityOctets = -1
        messages.add(buildMessage())
        state = ConsumeState.REQUEST_LINE
    }

    private fun handleChunkedEntity(messages: MutableList<HTTPRequestMessage>) {
        entityBuffer.flip()
        entity = HTTPEntity.Content(
            buffer = ByteBuffer
                .allocate(entityBuffer.limit())
                .put(entityBuffer)
                .flip()
                .asReadOnlyBuffer()
        )
        entityBuffer.clear()
        expectedChunkSizeOctets = -1
        readChunkSizeOctets = -1
        messages.add(buildMessage())
        state = ConsumeState.REQUEST_LINE
    }

    private fun handleRequestLine() {
        val requestLineString = requestLineBuffer.stringifyAndClear(Charsets.US_ASCII)
        val parsedRequestLine = parseRequestLine(requestLineString).getOrThrow()
        logger.debug("got request line: $parsedRequestLine")
        this.requestLine = parsedRequestLine
        state = ConsumeState.HEADERS
    }

    private fun handleHeaderLine(messages: MutableList<HTTPRequestMessage>) {
        val headerLine = headersBuffer.stringifyAndClear(Charsets.US_ASCII)
        when {
            headerLine.isNotEmpty() -> {
                val parsedHeader = parseHeaderLine(headerLine).getOrNull()
                if (parsedHeader != null) {
                    headers.add(parsedHeader)
                }
            }
            else -> handleEndOfHeaders(messages)
        }
    }

    private fun handleEndOfHeaders(messages: MutableList<HTTPRequestMessage>) {
        logger.debug("got headers: $headers")

        val transferEncoding = headers.getSingleOrNull(HTTPHeaderConstants.transferEncoding)
        val isChunked = transferEncoding?.rawValue?.equals(HTTPHeaderConstants.chunked) ?: false
        val contentLength = headers.getSingleOrNull(HTTPHeaderConstants.contentLength)?.rawValue

        val requestLine = this.requestLine ?: throw RuntimeException("no request line available")
        state = when {
            requestLine.method == "HEAD" -> {
                messages.add(buildMessage())
                ConsumeState.REQUEST_LINE
            }
            isChunked -> {
                chunkState = ChunkConsumeState.SIZE_LINE
                ConsumeState.CHUNKED_ENTITY
            }
            contentLength != null -> {
                this.expectedEntityOctets = contentLength.toIntOrNull()
                    ?: throw RuntimeException("failed to parse content length")
                if (this.expectedEntityOctets < 0 || this.expectedEntityOctets > entityBuffer.capacity()) {
                    throw RuntimeException("bad content length")
                }

                if (expectedEntityOctets == 0) {
                    this.entity = HTTPEntity.NoContent
                    messages.add(buildMessage())
                    state = ConsumeState.REQUEST_LINE
                    return
                }

                this.readEntityOctets = 0
                ConsumeState.FIXED_ENTITY
            }
            else -> {
                messages.add(buildMessage())
                ConsumeState.REQUEST_LINE
            }
        }
        logger.debug("state after headers: $state")
    }

    // Copies bytes from the source to the target buffer until LF is found
    // Returns true if a CRLF terminated line as been accumulated, false otherwise
    private fun consumeLine(source: ByteBuffer, target: ByteBuffer): Boolean {
        when (val nextLineFeedPosition = source.nextPositionOfOrNull(HTTPCharacters.LINE_FEED_BYTE)) {
            null -> {
                // No LF found in buffer - accumulate the entirety of source
                target.put(source)
                return false
            }
            0 -> {
                // LF at the start of the source buffer
                // Avoid a "zero copy" by skipping over it
                source.advance(1)
            }
            else -> {
                // LF found somewhere in the buffer - manually copy bytes preceding it
                val targetPosition = target.position()
                val sourcePosition = source.position()
                val length = nextLineFeedPosition - sourcePosition
                target.put(targetPosition, source, sourcePosition, length)
                target.position(targetPosition + length)
                source.advance(length + 1)
            }
        }

        target.flip()
        target.trimTrailing(HTTPCharacters.CARRIAGE_RETURN_BYTE)
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

        headers = HTTPHeaders()
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

        val split = line.split(HTTPCharacters.SPACE, limit = 3)
        if (split.size != 3) {
            return Result.failure(RuntimeException("expected request line to have 3 parts"))
        }

        val (method, resourceUri, httpVersion) = split
        // todo: verify method is a "token" entity
        if (method.isEmpty()) {
            return Result.failure(RuntimeException("malformed method"))
        }

        // todo: verify uri is ""*" | absoluteURI | abs_path | authority"
        if (resourceUri.isEmpty()) {
            return Result.failure(RuntimeException("malformed method"))
        }

        // todo: verify version is ""HTTP" "/" 1*DIGIT "." 1*DIGIT"
        if (httpVersion.isEmpty()) {
            return Result.failure(RuntimeException("malformed version"))
        }

        return Result.success(
            HTTPRequestLine(
                method = method,
                resourceUri = resourceUri,
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
