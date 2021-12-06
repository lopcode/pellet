package dev.pellet

import dev.pellet.codec.http.HTTPMethod
import dev.pellet.handler.PelletHTTPRouteHandling
import dev.pellet.routing.HTTPRoute
import dev.pellet.routing.PelletHTTPRouter

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
        val routes = builder.build()
        return PelletHTTPRouter(routes)
    }
}

@PelletBuilderDslTag
class RouterBuilder {

    val routes = mutableListOf<HTTPRoute>()

    fun get(path: String, handler: PelletHTTPRouteHandling) {
        routes.add(HTTPRoute(HTTPMethod.Get, path, handler))
    }

    fun post(path: String, handler: PelletHTTPRouteHandling) {
        routes.add(HTTPRoute(HTTPMethod.Post, path, handler))
    }

    internal fun build(): List<HTTPRoute> {
        return routes
    }
}

@PelletBuilderDslTag
class PelletServerBuilder {

    val connectors = mutableListOf<PelletConnector>()

    fun httpConnector(lambda: (@PelletBuilderDslTag HTTPConnectorBuilder).() -> Unit) {
        val builder = HTTPConnectorBuilder()
        lambda(builder)
        connectors.add(builder.build())
    }

    internal fun build(): PelletServer {
        return PelletServer(connectors)
    }
}

@PelletBuilderDslTag
class HTTPConnectorBuilder {

    lateinit var endpoint: PelletConnector.Endpoint
    lateinit var router: PelletHTTPRouter

    fun router(lambda: (@PelletBuilderDslTag RouterBuilder).() -> Unit) {
        val builder = RouterBuilder()
        lambda(builder)
        val routes = builder.build()
        router = PelletHTTPRouter(routes)
    }

    internal fun build(): PelletConnector {
        return PelletConnector.HTTP(
            endpoint,
            router
        )
    }
}
