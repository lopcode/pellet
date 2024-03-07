package dev.pellet.server

import dev.pellet.server.PelletBuilder.httpRouter
import dev.pellet.server.codec.http.HTTPMethod
import dev.pellet.server.responder.http.PelletHTTPRouteContext
import dev.pellet.server.routing.http.HTTPRouteResponse
import dev.pellet.server.routing.http.PelletHTTPRoute
import dev.pellet.server.routing.http.PelletHTTPRoutePath
import dev.pellet.server.routing.uuidDescriptor
import kotlin.test.Test
import kotlin.test.assertEquals

class PelletRouteBuilderTests {

    @Test
    fun `sense check no routes`() {
        val sut = httpRouter {}

        val expectedRoutes = listOf<PelletHTTPRoute>()
        assertEquals(expectedRoutes, sut.routes)
    }

    @Test
    fun `sense check empty path`() {
        val sut = httpRouter {
            path("hello") {
            }
        }

        val expectedRoutes = listOf<PelletHTTPRoute>()
        assertEquals(expectedRoutes, sut.routes)
    }

    @Test
    fun `get raw path`() {
        val sut = httpRouter {
            get("/", ::handler)
        }

        val expectedRoutes = listOf(
            PelletHTTPRoute(
                HTTPMethod.Get,
                PelletHTTPRoutePath.parse("/"),
                ::handler
            )
        )
        assertEquals(expectedRoutes, sut.routes)
    }

    @Test
    fun `get route path`() {
        val routePath = PelletHTTPRoutePath.parse("/hello/world")
        val sut = httpRouter {
            get(routePath, ::handler)
        }

        val expectedRoutes = listOf(
            PelletHTTPRoute(
                HTTPMethod.Get,
                routePath,
                ::handler
            )
        )
        assertEquals(expectedRoutes, sut.routes)
    }

    @Test
    fun `get descriptor path`() {
        val idDescriptor = uuidDescriptor("id")
        val idTwoDescriptor = uuidDescriptor("idtwo")
        val sut = httpRouter {
            get(idDescriptor, ::handler)
            path(idDescriptor) {
                get(idTwoDescriptor, ::handler)
            }
        }

        val expectedFirstRoute = PelletHTTPRoutePath.Builder()
            .addVariable(idDescriptor)
            .build()
        val expectedSecondRoute = PelletHTTPRoutePath.Builder()
            .addVariable(idDescriptor)
            .addVariable(idTwoDescriptor)
            .build()
        val expectedRoutes = listOf(
            PelletHTTPRoute(
                HTTPMethod.Get,
                expectedFirstRoute,
                ::handler
            ),
            PelletHTTPRoute(
                HTTPMethod.Get,
                expectedSecondRoute,
                ::handler
            )
        )
        assertEquals(expectedRoutes, sut.routes)
    }

    @Test
    fun `multiple nested paths`() {
        val sut = httpRouter {
            path("/hello") {
                path("world") {
                    path("//test") {
                        // todo: does this make sense to match /hello/world/test ?
                        get("", ::handler)
                    }
                }
            }
        }

        val expectedRoute = PelletHTTPRoutePath.parse("/hello/world/test/")
        val expectedRoutes = listOf(
            PelletHTTPRoute(
                HTTPMethod.Get,
                expectedRoute,
                ::handler
            )
        )
        assertEquals(expectedRoutes, sut.routes)
    }

    @Test
    fun `sense check methods`() {
        val sut = httpRouter {
            get("/", ::handler)
            put("/", ::handler)
            post("/", ::handler)
            patch("/", ::handler)
            delete("/", ::handler)
            route(HTTPMethod.Head, "/", ::handler)
            route(HTTPMethod.Connect, "/", ::handler)
            route(HTTPMethod.Options, "/", ::handler)
            route(HTTPMethod.Trace, "/", ::handler)
            route(HTTPMethod.Custom("HELLO"), "/", ::handler)
        }

        val expectedMethods = listOf(
            HTTPMethod.Get,
            HTTPMethod.Put,
            HTTPMethod.Post,
            HTTPMethod.Patch,
            HTTPMethod.Delete,
            HTTPMethod.Head,
            HTTPMethod.Connect,
            HTTPMethod.Options,
            HTTPMethod.Trace,
            HTTPMethod.Custom("HELLO")
        )
        val expectedRoutes = expectedMethods.map {
            PelletHTTPRoute(
                it,
                PelletHTTPRoutePath.parse("/"),
                ::handler
            )
        }
        assertEquals(expectedRoutes, sut.routes)
    }

    private fun handler(
        context: PelletHTTPRouteContext
    ): HTTPRouteResponse {
        return HTTPRouteResponse.Builder()
            .noContent()
            .build()
    }
}
