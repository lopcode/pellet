package dev.pellet

import dev.pellet.codec.http.HTTPMethod
import dev.pellet.routing.http.PelletHTTPRoute
import dev.pellet.routing.http.PelletHTTPRouteHandling
import dev.pellet.routing.http.PelletHTTPRouter
import java.net.URI

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

    fun get(path: String, handler: PelletHTTPRouteHandling) {
        val uri = URI.create(path)
        router.add(PelletHTTPRoute(HTTPMethod.Get, uri, handler))
    }

    fun post(path: String, handler: PelletHTTPRouteHandling) {
        val uri = URI.create(path)
        router.add(PelletHTTPRoute(HTTPMethod.Post, uri, handler))
    }

    internal fun build(): PelletHTTPRouter {
        return router
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
        router = builder.build()
    }

    internal fun build(): PelletConnector {
        return PelletConnector.HTTP(
            endpoint,
            router
        )
    }
}
