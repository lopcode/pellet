package dev.pellet.server.codec

import dev.pellet.server.PelletServerClient
import dev.pellet.server.codec.http.HTTPRequestMessage
import kotlin.test.assertEquals

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
        assertEquals(expected, spyMessages.first())
    }

    fun assertMessagesEqual(vararg expected: HTTPRequestMessage) {
        val expectedList = expected.toList()
        assertEquals(expectedList, spyMessages)
    }
}
