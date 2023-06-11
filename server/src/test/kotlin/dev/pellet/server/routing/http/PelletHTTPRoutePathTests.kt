package dev.pellet.server.routing.http

import kotlin.test.Test
import kotlin.test.assertEquals

class PelletHTTPRoutePathTests {

    @Test
    fun `empty route path shows as slash`() {
        val sut = PelletHTTPRoutePath(
            components = listOf()
        )

        val result = sut.toString()

        assertEquals("/", result)
    }

    @Test
    fun `toString same as path`() {
        val sut = PelletHTTPRoutePath(
            components = listOf(
                PelletHTTPRoutePath.Component.Plain("hello"),
                PelletHTTPRoutePath.Component.Plain("world")
            )
        )

        val result = sut.path

        assertEquals("/hello/world", result)
    }

    @Test
    fun `plain route path shows correctly`() {
        val sut = PelletHTTPRoutePath(
            components = listOf(
                PelletHTTPRoutePath.Component.Plain("hello"),
                PelletHTTPRoutePath.Component.Plain("world")
            )
        )

        val result = sut.toString()

        assertEquals("/hello/world", result)
    }

    @Test
    fun `variable without visual type shows without suffix`() {
        val sut = PelletHTTPRoutePath(
            components = listOf(
                PelletHTTPRoutePath.Component.Variable(
                    "variable",
                    visualType = null
                )
            )
        )

        val result = sut.toString()

        assertEquals("/{variable}", result)
    }

    @Test
    fun `variable with visual type shows suffix`() {
        val sut = PelletHTTPRoutePath(
            components = listOf(
                PelletHTTPRoutePath.Component.Variable(
                    "variable",
                    visualType = "visual_type"
                )
            )
        )

        val result = sut.toString()

        assertEquals("/{variable:visual_type}", result)
    }
}
