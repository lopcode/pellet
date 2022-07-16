package dev.pellet.server.codec.http

import dev.pellet.server.MockPelletServerClient
import dev.pellet.server.PelletServerClient
import dev.pellet.server.buffer.AlwaysAllocatingPelletBufferPool
import dev.pellet.server.buffer.PelletBuffer
import dev.pellet.server.buffer.PelletBufferPooling
import dev.pellet.server.codec.MockHTTPCodecHandler
import java.net.URI
import kotlin.test.BeforeTest
import kotlin.test.Test

class HTTPMessageCodecTests {

    private val pool = AlwaysAllocatingPelletBufferPool(4096)
    private lateinit var mockClient: PelletServerClient
    private lateinit var mockHandler: MockHTTPCodecHandler
    private lateinit var sut: HTTPMessageCodec

    @BeforeTest
    fun setUp() {
        mockClient = MockPelletServerClient()
        mockHandler = MockHTTPCodecHandler()
        sut = HTTPMessageCodec(pool, mockHandler::handle)
    }

    @Test
    fun `most basic message sense check`() {
        val buffer = pool.bufferOf("GET / HTTP/1.1\r\n\r\n")

        sut.consume(buffer, mockClient)

        val expected = buildMessage(
            method = HTTPMethod.Get,
            resourceUri = "/"
        )
        mockHandler.assertMessageEquals(expected)
    }

    @Test
    fun `two simple messages`() {
        val bufferOne = pool.bufferOf("GET /one HTTP/1.1\r\n\r\n")
        val bufferTwo = pool.bufferOf("GET /two HTTP/1.1\r\n\r\n")

        sut.consume(bufferOne, mockClient)
        sut.consume(bufferTwo, mockClient)

        val expectedOne = buildMessage(
            method = HTTPMethod.Get,
            resourceUri = "/one"
        )
        val expectedTwo = buildMessage(
            method = HTTPMethod.Get,
            resourceUri = "/two"
        )
        mockHandler.assertMessagesEqual(expectedOne, expectedTwo)
    }

    @Test
    fun `complex fixed entity message sense check`() {
        val complexMessage = "POST /post HTTP/1.1\r\n" +
            "Hello: World\r\n" +
            "Content-Length: 4\r\n\r\n" +
            "1234"
        val buffer = pool.bufferOf(complexMessage)

        sut.consume(buffer, mockClient)

        val expected = buildMessage(
            method = HTTPMethod.Post,
            resourceUri = "/post",
            headers = HTTPHeaders()
                .add("Hello", "World")
                .add("Content-Length", "4"),
            entity = HTTPEntity.Content(
                pool.bufferOf("1234")
            )
        )
        mockHandler.assertMessageEquals(expected)
    }

    @Test
    fun `complex chunked entity message sense check`() {
        val complexMessage = "POST /post HTTP/1.1\r\n" +
            "Hello: World\r\n" +
            "Transfer-Encoding: chunked\r\n\r\n" +
            "1\r\n" + // size: 1 octet
            "1\r\n" + // data: "1"
            "3\r\n" + // size: 3 octets
            "234\r\n" + // data: "234"
            "0\r\n" + // end size chunk
            "\r\n"
        val buffer = pool.bufferOf(complexMessage)

        sut.consume(buffer, mockClient)

        val expected = buildMessage(
            method = HTTPMethod.Post,
            resourceUri = "/post",
            headers = HTTPHeaders()
                .add("Hello", "World")
                .add("Transfer-Encoding", "chunked"),
            entity = HTTPEntity.Content(
                pool.bufferOf("1234")
            )
        )
        mockHandler.assertMessageEquals(expected)
    }

    @Test
    fun `two complex chunked entity messages`() {
        val complexMessageOne = "POST /hello HTTP/1.1\r\n" +
            "Hello: World\r\n" +
            "Transfer-Encoding: chunked\r\n\r\n" +
            "1\r\n" + // size: 1 octet
            "H\r\n" + // data: "H"
            "5\r\n" + // size: 5 octets
            "ello \r\n" + // data: "ello "
            "0\r\n" + // end size chunk
            "\r\n"
        val complexMessageTwo = "POST /world HTTP/1.1\r\n" +
            "Hello: World\r\n" +
            "Transfer-Encoding: chunked\r\n\r\n" +
            "2\r\n" + // size: 2 octets
            "Wo\r\n" + // data: "Wo"
            "2\r\n" + // size: 2 octets
            "rl\r\n" + // data: "rl"
            "2\r\n" + // size: 2 octets
            "d!\r\n" + // data: "d!"
            "0\r\n" + // end size chunk
            "\r\n"
        val bufferOne = pool.bufferOf(complexMessageOne)
        val bufferTwo = pool.bufferOf(complexMessageTwo)

        sut.consume(bufferOne, mockClient)
        sut.consume(bufferTwo, mockClient)

        val expectedOne = buildMessage(
            method = HTTPMethod.Post,
            resourceUri = "/hello",
            headers = HTTPHeaders()
                .add("Hello", "World")
                .add("Transfer-Encoding", "chunked"),
            entity = HTTPEntity.Content(
                pool.bufferOf("Hello ")
            )
        )
        val expectedTwo = buildMessage(
            method = HTTPMethod.Post,
            resourceUri = "/world",
            headers = HTTPHeaders()
                .add("Hello", "World")
                .add("Transfer-Encoding", "chunked"),
            entity = HTTPEntity.Content(
                pool.bufferOf("World!")
            )
        )
        mockHandler.assertMessagesEqual(expectedOne, expectedTwo)
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
}
