package dev.pellet.server.routing.http

import dev.pellet.server.codec.http.HTTPEntity
import dev.pellet.server.codec.http.HTTPHeaders
import dev.pellet.server.codec.http.HTTPMethod
import dev.pellet.server.codec.http.HTTPRequestLine
import dev.pellet.server.codec.http.HTTPRequestMessage
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

class PelletHTTPRouterTests {

    @Test
    fun `empty route table results in no route`() {
        val sut = PelletHTTPRouter()
        val message = stubMessage("/")

        val result = sut.route(message)

        assertNull(result)
    }

    @Test
    fun `two identical routes matches first instance`() {
        val sut = PelletHTTPRouter()
        val routeOne = stubRoute("/")
        val routeTwo = stubRoute("/")
        sut.add(routeOne)
        sut.add(routeTwo)
        val message = stubMessage("/")

        val result = sut.route(message)

        assertNotNull(result)
        assertSame(result.route, routeOne)
    }

    @Test
    fun `single simple get route resolves correctly`() {
        val sut = PelletHTTPRouter()
        val route = stubRoute("/")
        sut.add(route)
        val message = stubMessage("/")

        val result = sut.route(message)

        val expectedResult = HTTPRouting.ResolvedRoute(
            route = route,
            valueMap = mapOf()
        )
        assertEquals(expectedResult, result)
    }

    @Test
    fun `single get route with variables resolves correctly`() {
        val sut = PelletHTTPRouter()
        val routePath = PelletHTTPRoutePath.Builder()
            .addComponents("hello")
            .addVariable("first_variable")
            .addVariable("second_variable")
            .build()
        val route = PelletHTTPRoute(
            method = HTTPMethod.Get,
            routePath = routePath,
            handler = MockHTTPRouteHandler()
        )
        sut.add(route)
        val message = stubMessage("/hello/wor/ld")

        val result = sut.route(message)

        val expectedResult = HTTPRouting.ResolvedRoute(
            route = route,
            valueMap = mapOf(
                "first_variable" to "wor",
                "second_variable" to "ld"
            )
        )
        assertEquals(expectedResult, result)
    }

    @Test
    fun `router with nested routes matches as expected`() {
        val sut = PelletHTTPRouter()
        val routeRoot = stubRoute("/")
        val routeOne = stubRoute("/one")
        val routeTwo = stubRoute("/one/two")
        val routeThree = stubRoute("/one/two/three")
        val routeTwoOther = stubRoute("/one/two/other")
        sut.add(routeRoot)
        sut.add(routeOne)
        sut.add(routeTwo)
        sut.add(routeTwoOther)
        sut.add(routeThree)
        val testCases = listOf(
            "/" to HTTPRouting.ResolvedRoute(
                route = routeRoot,
                valueMap = mapOf()
            ),
            "/one" to HTTPRouting.ResolvedRoute(
                route = routeOne,
                valueMap = mapOf()
            ),
            "/one/two" to HTTPRouting.ResolvedRoute(
                route = routeTwo,
                valueMap = mapOf()
            ),
            "/one/two/three" to HTTPRouting.ResolvedRoute(
                route = routeThree,
                valueMap = mapOf()
            ),
            "/one/two/other" to HTTPRouting.ResolvedRoute(
                route = routeTwoOther,
                valueMap = mapOf()
            ),
            "/one/two/three/non_existent" to null
        )

        testCases.forEach { (path, expectedResult) ->
            val message = stubMessage(path)

            val result = sut.route(message)

            assertEquals(expectedResult, result)
        }
    }

    private fun stubRoute(
        path: String
    ): PelletHTTPRoute {
        return PelletHTTPRoute(
            method = HTTPMethod.Get,
            routePath = PelletHTTPRoutePath.parse(path),
            handler = MockHTTPRouteHandler()
        )
    }

    private fun stubMessage(
        path: String
    ): HTTPRequestMessage {
        return HTTPRequestMessage(
            requestLine = HTTPRequestLine(
                method = HTTPMethod.Get,
                resourceUri = URI.create(path),
                httpVersion = ""
            ),
            headers = HTTPHeaders(),
            entity = HTTPEntity.NoContent
        )
    }
}
