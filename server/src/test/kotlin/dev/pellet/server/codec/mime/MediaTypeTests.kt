package dev.pellet.server.codec.mime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MediaTypeTests {

    @Test
    fun `vararg constructor maintains parameters list`() {
        val sut = MediaType("a", "b", "c" to "d", "e" to "f")
        val expected = MediaType("a", "b", listOf("c" to "d", "e" to "f"))
        assertEquals(expected, sut)
    }

    @Test
    fun `no charset results in null`() {
        val testCases = listOf(
            MediaType("text", "plain"),
            MediaType("text", "plain", "a" to "b")
        )
        testCases.forEach { sut ->
            val result = sut.charsetOrNull()
            assertNull(result)
        }
    }

    @Test
    fun `charset with multiple charsets returns the first`() {
        val sut = MediaType(
            "text",
            "plain",
            listOf(
                "charset" to "utf-8",
                "charset" to "us-ascii"
            )
        )

        val result = sut.charsetOrNull()

        assertEquals(Charsets.UTF_8, result)
    }

    @Test
    fun `invalid charset returns null`() {
        val sut = MediaType("text", "plain", "charset" to "not a charset")

        val result = sut.charsetOrNull()

        assertNull(result)
    }
}
