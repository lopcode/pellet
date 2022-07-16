package dev.pellet.server.codec.mime

import dev.pellet.assertFailureIs
import dev.pellet.assertSuccess
import dev.pellet.server.codec.ParseException
import kotlin.test.Test

class MediaTypeParserTests {

    @Test
    fun `empty media type fails`() {
        val testCase = ""
        val result = MediaTypeParser.parse(testCase)
        assertFailureIs<ParseException>(result)
    }

    @Test
    fun `simple media type`() {
        val testCase = "text/plain"
        val result = MediaTypeParser.parse(testCase)
        val expected = MediaType(
            type = "text",
            subtype = "plain",
            parameters = listOf()
        )
        assertSuccess(expected, result)
    }

    @Test
    fun `simple media type with parameter`() {
        val testCase = "application/json; charset=utf-8"
        val result = MediaTypeParser.parse(testCase)
        val expected = MediaType(
            type = "application",
            subtype = "json",
            parameters = listOf(
                "charset" to "utf-8"
            )
        )
        assertSuccess(expected, result)
    }

    @Test
    fun `optional whitespace doesn't affect output`() {
        val testCase = "application/json   ;   charset=utf-8; something=else"
        val result = MediaTypeParser.parse(testCase)
        val expected = MediaType(
            type = "application",
            subtype = "json",
            parameters = listOf(
                "charset" to "utf-8",
                "something" to "else"
            )
        )
        assertSuccess(expected, result)
    }

    @Test
    fun `quotation marks for parameter value`() {
        val testCase = "application/json; charset=\"utf-8\""
        val result = MediaTypeParser.parse(testCase)
        val expected = MediaType(
            type = "application",
            subtype = "json",
            parameters = listOf(
                "charset" to "utf-8",
            )
        )
        assertSuccess(expected, result)
    }

    @Test
    fun `mismatched quotation marks for parameter value are preserved`() {
        val testCase = "application/json; charset=\"utf-8"
        val result = MediaTypeParser.parse(testCase)
        val expected = MediaType(
            type = "application",
            subtype = "json",
            parameters = listOf(
                "charset" to "\"utf-8",
            )
        )
        assertSuccess(expected, result)
    }

    @Test
    fun `invalid media types`() {
        val testCases = listOf(
            " ",
            "text/plain/something",
            "text plain;",
            "text/plain;;;",
            "text/plain;==",
            "text/plain;a=",
            "text/",
            "/plain",
            ";",
            ";/",
            "/;",
        )
        testCases.forEach { testCase ->
            val result = MediaTypeParser.parse(testCase)
            assertFailureIs<ParseException>(result)
        }
    }
}
