package dev.pellet.server.codec

import dev.pellet.server.PelletServerClient
import dev.pellet.server.codec.http.HTTPEntity
import dev.pellet.server.codec.http.HTTPRequestMessage
import kotlinx.io.snapshot
import kotlin.test.assertEquals
import kotlin.test.fail

class MockHTTPCodecHandler {

    val spyMessages = mutableListOf<HTTPRequestMessage>()

    fun handle(
        message: HTTPRequestMessage,
        client: PelletServerClient
    ) {
        spyMessages.add(message)
    }

    fun assertMessageEquals(expected: HTTPRequestMessage) {
        assertEquals(1, spyMessages.size)
        assertMessagesEqual(expected, spyMessages.first())
    }

    fun assertMessagesEqual(vararg expected: HTTPRequestMessage) {
        val expectedList = expected.toList()
        assertEquals(expectedList.size, spyMessages.size)
        val both = expectedList.zip(spyMessages)
        both.forEach { (expected, actual) ->
            assertMessagesEqual(expected, actual)
        }
    }

    /**
     * todo: does this make sense to make part of public api for [HTTPRequestMessage]?
     */
    private fun assertMessagesEqual(
        expected: HTTPRequestMessage,
        actual: HTTPRequestMessage
    ) {
        assertEquals(expected.requestLine, actual.requestLine)
        assertEquals(expected.headers, actual.headers)

        if (expected.entity is HTTPEntity.NoContent && actual.entity is HTTPEntity.NoContent) {
            return
        }

        if (expected.entity !is HTTPEntity.Content) {
            fail("unexpected entity type")
        }

        if (actual.entity !is HTTPEntity.Content) {
            fail("unexpected actual entity type")
        }

        val expectedBuffer = (expected.entity as HTTPEntity.Content).buffer
        val actualBuffer = (actual.entity as HTTPEntity.Content).buffer

        // note that this might be expensive for large entities
        assertEquals(expectedBuffer.snapshot(), actualBuffer.snapshot())
    }
}
