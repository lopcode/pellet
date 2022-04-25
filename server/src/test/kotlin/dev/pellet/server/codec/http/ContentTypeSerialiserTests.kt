package dev.pellet.server.codec.http

import kotlin.test.Test
import kotlin.test.assertEquals

class ContentTypeSerialiserTests {

    @Test
    fun `simple content type`() {
        val test = ContentType(type = "application", subtype = "json")
        val result = ContentTypeSerialiser.serialise(test)
        assertEquals("application/json", result)
    }

    @Test
    fun `content type with single parameter`() {
        val test = ContentType(
            type = "application",
            subtype = "json",
            parameters = listOf(
                "charset" to "utf-8"
            )
        )
        val result = ContentTypeSerialiser.serialise(test)
        assertEquals("application/json;charset=\"utf-8\"", result)
    }

    @Test
    fun `content type with multiple parameters`() {
        val test = ContentType(
            type = "application",
            subtype = "json",
            parameters = listOf(
                "charset" to "utf-8",
                "something" to "else"
            )
        )
        val result = ContentTypeSerialiser.serialise(test)
        assertEquals("application/json;charset=\"utf-8\";something=\"else\"", result)
    }
}
