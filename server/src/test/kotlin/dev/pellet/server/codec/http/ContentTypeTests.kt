package dev.pellet.server.codec.http

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContentTypeTests {

    @Test
    fun `wildcard root type matches any other content type`() {
        val sut = ContentType("*", "*")

        val result = sut.matches(
            ContentType("anything", "anything")
        )

        assertTrue(result)
    }

    @Test
    fun `mismatching root type does not match`() {
        val sut = ContentType("a", "*")

        val result = sut.matches(
            ContentType("b", "anything")
        )

        assertFalse(result)
    }

    @Test
    fun `wildcard subtype matches other subtype`() {
        val sut = ContentType("something", "*")

        val result = sut.matches(
            ContentType("something", "else")
        )

        assertTrue(result)
    }

    @Test
    fun `mismatching subtype does not match`() {
        val sut = ContentType("something", "else")

        val result = sut.matches(
            ContentType("something", "another else")
        )

        assertFalse(result)
    }

    @Test
    fun `mismatching parameters does not match`() {
        val sut = ContentType("type", "subtype", "a" to "b")

        val result = sut.matches(
            ContentType("type", "subtype", "a" to "c")
        )

        assertFalse(result)
    }

    @Test
    fun `all parts identical matches`() {
        val sut = ContentType("type", "subtype", "a" to "b")

        val result = sut.matches(
            ContentType("type", "subtype", "a" to "b")
        )

        assertTrue(result)
    }
}
