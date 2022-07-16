package dev.pellet.server.codec.http

import dev.pellet.assertFailure
import dev.pellet.assertSuccess
import dev.pellet.server.codec.http.query.QueryParameters
import dev.pellet.server.codec.http.query.QueryParser
import kotlin.test.Test

class QueryParserTests {

    @Test
    fun `no query`() {
        val query = ""
        val parsed = QueryParser.parseEncodedQuery(query)
        val expected = QueryParameters(
            mapOf()
        )
        assertSuccess(expected, parsed)
    }

    @Test
    fun `single basic parameter`() {
        val query = "a=b"
        val parsed = QueryParser.parseEncodedQuery(query)
        val expected = QueryParameters(
            mapOf(
                "a" to listOf("b")
            )
        )
        assertSuccess(expected, parsed)
    }

    @Test
    fun `multiple basic parameters`() {
        val query = "a=b&c=d"
        val parsed = QueryParser.parseEncodedQuery(query)
        val expected = QueryParameters(
            mapOf(
                "a" to listOf("b"),
                "c" to listOf("d")
            )
        )
        assertSuccess(expected, parsed)
    }

    @Test
    fun `multiple named parameters with the same name form an ordered list`() {
        val query = "a=b&a=c"
        val parsed = QueryParser.parseEncodedQuery(query)
        val expected = QueryParameters(
            mapOf(
                "a" to listOf("b", "c")
            )
        )
        assertSuccess(expected, parsed)
    }

    @Test
    fun `mixed null and empty parameters`() {
        val query = "a=&c=d&e"
        val parsed = QueryParser.parseEncodedQuery(query)
        val expected = QueryParameters(
            mapOf(
                "a" to listOf(""),
                "c" to listOf("d"),
                "e" to listOf(null),
            )
        )
        assertSuccess(expected, parsed)
    }

    @Test
    fun `mixed equals`() {
        val query = "a=b=c=d"
        val parsed = QueryParser.parseEncodedQuery(query)
        val expected = QueryParameters(
            mapOf(
                "a" to listOf("b=c=d"),
            )
        )
        assertSuccess(expected, parsed)
    }

    @Test
    fun `every kind of parameter`() {
        val query = "a=%26&b&c=&d=hello"
        val parsed = QueryParser.parseEncodedQuery(query)
        val expected = QueryParameters(
            mapOf(
                "a" to listOf("&"),
                "b" to listOf(null),
                "c" to listOf(""),
                "d" to listOf("hello"),
            )
        )
        assertSuccess(expected, parsed)
    }

    @Test
    fun `invalid query results in a failure`() {
        val queries = listOf(
            "a=%x",
            "&",
            "&&",
            "a=%x&c=d",
            "%x=b",
            "%x",
            "=",
            "==",
            "&=&=",
        )
        queries.forEach { query ->
            val result = QueryParser.parseEncodedQuery(query)
            assertFailure(result)
        }
    }
}
