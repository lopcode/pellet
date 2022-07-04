package dev.pellet.server.codec.http

import dev.pellet.logging.pelletLogger
import dev.pellet.server.PelletServerClient
import dev.pellet.server.buffer.PelletBuffer
import dev.pellet.server.buffer.PelletBufferPooling
import dev.pellet.server.codec.Codec
import dev.pellet.server.extension.advance
import dev.pellet.server.extension.nextPositionOfOrNull
import dev.pellet.server.extension.stringifyAndClear
import dev.pellet.server.extension.trimLWS
import dev.pellet.server.extension.trimTrailing
import java.net.URI
import java.util.Locale
import java.util.concurrent.ArrayBlockingQueue
import kotlin.math.min

internal class HTTPMessageCodec(
    private val pool: PelletBufferPooling,
    private val workQueue: ArrayBlockingQueue<IncomingMessageWorkItem>
) : Codec {

    private val requestLineBuffer = pool.provide()
    private val headersBuffer = pool.provide()

    private var expectedEntityOctets: Int = -1
    private var readEntityOctets: Int = -1
    private val entityBuffer = pool.provide()
    private val chunkLineBuffer = pool.provide()
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

    override fun release() {
        pool.release(requestLineBuffer)
        pool.release(headersBuffer)
        pool.release(entityBuffer)
        pool.release(chunkLineBuffer)
    }

    override fun consume(
        buffer: PelletBuffer,
        client: PelletServerClient
    ) {
        readLoop@ while (buffer.hasRemaining()) {
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
                        workQueue.put(IncomingMessageWorkItem(buildMessage(), client))
                    }
                }
                ConsumeState.FIXED_ENTITY -> {
                    if (!consumeFixedEntity(buffer, entityBuffer)) {
                        continue@readLoop
                    }

                    handleFixedEntity()
                    workQueue.put(IncomingMessageWorkItem(buildMessage(), client))
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
                            workQueue.put(IncomingMessageWorkItem(buildMessage(), client))
                        }
                    }
                }
            }
        }
    }

    private fun handleChunkSizeLine() {
        val expectedChunkSizeOctets = chunkLineBuffer
            .stringifyAndClear(Charsets.US_ASCII)
            .toInt(radix = 16)
        assert(expectedChunkSizeOctets >= 0)
        assert((expectedChunkSizeOctets + readChunkSizeOctets) < entityBuffer.capacity())
        when (expectedChunkSizeOctets) {
            0 -> {
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

    private fun consumeFixedEntity(source: PelletBuffer, target: PelletBuffer): Boolean {
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

    private fun consumeDataChunk(source: PelletBuffer, target: PelletBuffer): Boolean {
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

    private fun handleFixedEntity() {
        entityBuffer.flip()
        entity = HTTPEntity.Content(
            buffer = pool.provide()
                .limit(expectedEntityOctets)
                .put(entityBuffer)
                .flip()
        )
        entityBuffer.clear()
        expectedEntityOctets = -1
        readEntityOctets = -1
        state = ConsumeState.REQUEST_LINE
    }

    private fun handleChunkedEntity() {
        entityBuffer.flip()
        entity = HTTPEntity.Content(
            buffer = pool.provide()
                .limit(entityBuffer.limit())
                .put(entityBuffer)
                .flip()
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
                this.expectedEntityOctets = contentLength.toIntOrNull()
                    ?: throw RuntimeException("failed to parse content length")
                if (this.expectedEntityOctets < 0 || this.expectedEntityOctets > entityBuffer.capacity()) {
                    throw RuntimeException("bad content length")
                }

                if (expectedEntityOctets == 0) {
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
    private fun consumeLine(source: PelletBuffer, target: PelletBuffer): Boolean {
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
