package dev.pellet.server

import dev.pellet.server.codec.http.HTTPMethod
import dev.pellet.server.routing.RouteVariableDescriptor
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
        lambda: suspend (@PelletBuilderDslTag PelletRoutePathBuilder).() -> Unit
    ): PelletHTTPRouter {
        val router = PelletHTTPRouter()
        val routePath = PelletHTTPRoutePath(listOf())
        val builder = PelletRoutePathBuilder(router, routePath)
        lambda(builder)
        return router
    }
}

@PelletBuilderDslTag
class PelletRoutePathBuilder(
    private val router: PelletHTTPRouter,
    private val routePathPrefix: PelletHTTPRoutePath
) {

    fun route(
        method: HTTPMethod,
        routePath: PelletHTTPRoutePath,
        handler: PelletHTTPRouteHandling
    ) {
        val fullRoutePath = routePath.prefixedWith(routePathPrefix)
        router.add(PelletHTTPRoute(method, fullRoutePath, handler))
    }

    fun route(
        method: HTTPMethod,
        descriptor: RouteVariableDescriptor<*>,
        handler: PelletHTTPRouteHandling
    ) {
        val routePath = PelletHTTPRoutePath.Builder()
            .addVariable(descriptor)
            .build()
        route(method, routePath, handler)
    }

    fun route(
        method: HTTPMethod,
        rawRoutePath: String,
        handler: PelletHTTPRouteHandling
    ) {
        val routePath = PelletHTTPRoutePath.parse(rawRoutePath)
        route(method, routePath, handler)
    }

    fun get(
        routePath: PelletHTTPRoutePath,
        handler: PelletHTTPRouteHandling
    ) {
        route(HTTPMethod.Get, routePath, handler)
    }

    fun get(
        descriptor: RouteVariableDescriptor<*>,
        handler: PelletHTTPRouteHandling
    ) {
        route(HTTPMethod.Get, descriptor, handler)
    }

    fun get(
        path: String,
        handler: PelletHTTPRouteHandling
    ) {
        route(HTTPMethod.Get, path, handler)
    }

    fun post(
        routePath: PelletHTTPRoutePath,
        handler: PelletHTTPRouteHandling
    ) {
        route(HTTPMethod.Post, routePath, handler)
    }

    fun post(
        descriptor: RouteVariableDescriptor<*>,
        handler: PelletHTTPRouteHandling
    ) {
        route(HTTPMethod.Post, descriptor, handler)
    }

    fun post(
        path: String,
        handler: PelletHTTPRouteHandling
    ) {
        route(HTTPMethod.Post, path, handler)
    }

    fun put(
        routePath: PelletHTTPRoutePath,
        handler: PelletHTTPRouteHandling
    ) {
        route(HTTPMethod.Put, routePath, handler)
    }

    fun put(
        descriptor: RouteVariableDescriptor<*>,
        handler: PelletHTTPRouteHandling
    ) {
        route(HTTPMethod.Put, descriptor, handler)
    }

    fun put(
        path: String,
        handler: PelletHTTPRouteHandling
    ) {
        route(HTTPMethod.Put, path, handler)
    }

    fun patch(
        routePath: PelletHTTPRoutePath,
        handler: PelletHTTPRouteHandling
    ) {
        route(HTTPMethod.Patch, routePath, handler)
    }

    fun patch(
        descriptor: RouteVariableDescriptor<*>,
        handler: PelletHTTPRouteHandling
    ) {
        route(HTTPMethod.Patch, descriptor, handler)
    }

    fun patch(
        path: String,
        handler: PelletHTTPRouteHandling
    ) {
        route(HTTPMethod.Patch, path, handler)
    }

    fun delete(
        routePath: PelletHTTPRoutePath,
        handler: PelletHTTPRouteHandling
    ) {
        route(HTTPMethod.Delete, routePath, handler)
    }

    fun delete(
        descriptor: RouteVariableDescriptor<*>,
        handler: PelletHTTPRouteHandling
    ) {
        route(HTTPMethod.Delete, descriptor, handler)
    }

    fun delete(
        path: String,
        handler: PelletHTTPRouteHandling
    ) {
        route(HTTPMethod.Delete, path, handler)
    }

    suspend fun path(
        newPathPrefix: PelletHTTPRoutePath,
        lambda: suspend (@PelletBuilderDslTag PelletRoutePathBuilder).() -> Unit
    ) {
        val fullPathPrefix = newPathPrefix.prefixedWith(routePathPrefix)
        val builder = PelletRoutePathBuilder(router, fullPathPrefix)
        lambda(builder)
    }

    suspend fun path(
        path: String,
        lambda: suspend (@PelletBuilderDslTag PelletRoutePathBuilder).() -> Unit
    ) {
        val parsedPathPrefix = PelletHTTPRoutePath.parse(path)
        path(parsedPathPrefix, lambda)
    }

    suspend fun path(
        descriptor: RouteVariableDescriptor<*>,
        lambda: suspend (@PelletBuilderDslTag PelletRoutePathBuilder).() -> Unit
    ) {
        val variablePathPrefix = PelletHTTPRoutePath.Builder()
            .addVariable(descriptor)
            .build()
        path(variablePathPrefix, lambda)
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

    fun router(lambda: (@PelletBuilderDslTag PelletRoutePathBuilder).() -> Unit) {
        val router = PelletHTTPRouter()
        val routePathPrefix = PelletHTTPRoutePath(listOf())
        val builder = PelletRoutePathBuilder(router, routePathPrefix)
        lambda(builder)
        this.router = router
    }

    internal fun build(): PelletConnector {
        return PelletConnector.HTTP(
            endpoint,
            router
        )
    }
}
