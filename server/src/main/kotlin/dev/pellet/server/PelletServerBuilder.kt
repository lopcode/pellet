package dev.pellet.server

import dev.pellet.server.codec.http.HTTPMethod
import dev.pellet.server.routing.http.PelletHTTPRoute
import dev.pellet.server.routing.http.PelletHTTPRouteHandling
import dev.pellet.server.routing.http.PelletHTTPRoutePath
import dev.pellet.server.routing.http.PelletHTTPRouter

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class PelletBuilderDslTag

@PelletBuilderDslTag
object PelletBuilder {

    fun pelletServer(
        lambda: (@PelletBuilderDslTag PelletServerBuilder).() -> Unit
    ): PelletServer {
        val builder = PelletServerBuilder()
        lambda(builder)
        return builder.build()
    }

    suspend fun httpRouter(
        lambda: suspend (@PelletBuilderDslTag RouterBuilder).() -> Unit
    ): PelletHTTPRouter {
        val builder = RouterBuilder()
        lambda(builder)
        return builder.build()
    }
}

@PelletBuilderDslTag
class RouterBuilder {

    private val router = PelletHTTPRouter()

    fun get(routePath: PelletHTTPRoutePath, handler: PelletHTTPRouteHandling) {
        router.add(PelletHTTPRoute(HTTPMethod.Get, routePath, handler))
    }

    fun get(path: String, handler: PelletHTTPRouteHandling) {
        val routePath = PelletHTTPRoutePath.parse(path)
        get(routePath, handler)
    }

    fun post(routePath: PelletHTTPRoutePath, handler: PelletHTTPRouteHandling) {
        router.add(PelletHTTPRoute(HTTPMethod.Post, routePath, handler))
    }

    fun post(path: String, handler: PelletHTTPRouteHandling) {
        val routePath = PelletHTTPRoutePath.parse(path)
        post(routePath, handler)
    }

    internal fun build(): PelletHTTPRouter {
        return router
    }
}

@PelletBuilderDslTag
class PelletServerBuilder {

    var logRequests: Boolean = true
    val connectors = mutableListOf<PelletConnector>()

    fun httpConnector(lambda: (@PelletBuilderDslTag HTTPConnectorBuilder).() -> Unit) {
        val builder = HTTPConnectorBuilder()
        lambda(builder)
        connectors.add(builder.build())
    }

    internal fun build(): PelletServer {
        return PelletServer(logRequests, connectors)
    }
}

@PelletBuilderDslTag
class HTTPConnectorBuilder {

    lateinit var endpoint: PelletConnector.Endpoint
    lateinit var router: PelletHTTPRouter

    fun router(lambda: (@PelletBuilderDslTag RouterBuilder).() -> Unit) {
        val builder = RouterBuilder()
        lambda(builder)
        router = builder.build()
    }

    internal fun build(): PelletConnector {
        return PelletConnector.HTTP(
            endpoint,
            router
        )
    }
}
