package dev.pellet

import dev.pellet.codec.http.HTTPMethod
import dev.pellet.handler.PelletHTTPRouteHandling
import dev.pellet.routing.HTTPRoute
import dev.pellet.routing.PelletHTTPRouter

object PelletBuilder {

    fun pelletServer(
        lambda: PelletServerBuilder.() -> Unit
    ): PelletServer {
        val builder = PelletServerBuilder()
        lambda(builder)
        return builder.build()
    }

    suspend fun httpRouter(
        lambda: suspend RouterBuilder.() -> Unit
    ): PelletHTTPRouter {
        val builder = RouterBuilder()
        lambda(builder)
        val routes = builder.build()
        return PelletHTTPRouter(routes)
    }
}

class RouterBuilder {

    val routes = mutableListOf<HTTPRoute>()

    fun get(path: String, handler: PelletHTTPRouteHandling) {
        routes.add(HTTPRoute(HTTPMethod.Get, path, handler))
    }

    fun post(path: String, handler: PelletHTTPRouteHandling) {
        routes.add(HTTPRoute(HTTPMethod.Post, path, handler))
    }

    fun build(): List<HTTPRoute> {
        return routes
    }
}

class PelletServerBuilder {

    val connectors = mutableListOf<PelletConnector>()

    fun httpConnector(lambda: HTTPConnectorBuilder.() -> Unit) {
        val builder = HTTPConnectorBuilder()
        lambda(builder)
        connectors.add(builder.build())
    }

    fun build(): PelletServer {
        return PelletServer(connectors)
    }
}

class HTTPConnectorBuilder {

    lateinit var endpoint: PelletConnector.Endpoint
    lateinit var router: PelletHTTPRouter

    fun endpoint(lambda: EndpointBuilder.() -> Unit) {
        val builder = EndpointBuilder()
        lambda(builder)
        endpoint = builder.build()
    }

    fun router(lambda: RouterBuilder.() -> Unit) {
        val builder = RouterBuilder()
        lambda(builder)
        val routes = builder.build()
        router = PelletHTTPRouter(routes)
    }

    fun build(): PelletConnector {
        return PelletConnector.HTTP(
            endpoint,
            router
        )
    }
}

class EndpointBuilder {

    lateinit var hostname: String
    var port: Int = 0

    fun build(): PelletConnector.Endpoint {
        return PelletConnector.Endpoint(
            hostname,
            port
        )
    }
}
