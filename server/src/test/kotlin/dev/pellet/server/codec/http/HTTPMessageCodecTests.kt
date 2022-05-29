package dev.pellet.server.codec.http

import dev.pellet.server.buffer.AlwaysAllocatingPelletBufferPool
import dev.pellet.server.buffer.PelletBuffer
import dev.pellet.server.buffer.PelletBufferPooling
import kotlinx.coroutines.runBlocking
import java.net.URI
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class HTTPMessageCodecTests {

    private lateinit var pool: PelletBufferPooling
    private lateinit var sut: HTTPMessageCodec
    private lateinit var mockCodecHandler: MockCodecHandler<HTTPRequestMessage>

    @BeforeTest
    fun setUp() {
        pool = AlwaysAllocatingPelletBufferPool(allocationSize = 1024)
        mockCodecHandler = MockCodecHandler()
        sut = HTTPMessageCodec(
            mockCodecHandler,
            pool
        )
    }

    @Test
    fun `empty GET request`() = runBlocking {
        val message = pool.bufferOf("GET / HTTP/1.1\r\n\r\n")

        sut.consume(message)

        val expectedMessage = buildMessage(
            method = HTTPMethod.Get,
            resourceUri = "/",
            headers = HTTPHeaders(),
            entity = HTTPEntity.NoContent
        )
        assertMessage(expectedMessage)
    }

    @Test
    fun `GET with headers`() = runBlocking {
        val message = pool.bufferOf("GET / HTTP/1.1\r\nTest-Key: Value\r\nTest-Key2: Value2\r\n\r\n")

        sut.consume(message)

        val headers = HTTPHeaders()
            .set("Test-Key", "Value")
            .set("Test-Key2", "Value2")
        val expectedMessage = buildMessage(
            method = HTTPMethod.Get,
            resourceUri = "/",
            headers = headers,
            entity = HTTPEntity.NoContent
        )
        assertMessage(expectedMessage)
    }

    @Test
    fun `GET with headers and fixed entity`() = runBlocking {
        val message = pool.bufferOf("GET / HTTP/1.1\r\nTest-Key: Value\r\nContent-Length: 3\r\n\r\nabc")

        sut.consume(message)

        val headers = HTTPHeaders()
            .set("Test-Key", "Value")
            .set("Content-Length", "3")
        val expectedMessage = buildMessage(
            method = HTTPMethod.Get,
            resourceUri = "/",
            headers = headers,
            entity = HTTPEntity.Content(
                pool.bufferOf("abc")
            )
        )
        assertMessage(expectedMessage)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun assertMessage(
        expected: HTTPRequestMessage
    ) {
        assertEquals(mutableListOf(expected), mockCodecHandler.spyHandleOutput)
    }
}

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
