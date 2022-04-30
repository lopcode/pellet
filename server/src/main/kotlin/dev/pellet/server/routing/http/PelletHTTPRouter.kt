package dev.pellet.server.routing.http

import dev.pellet.logging.pelletLogger
import dev.pellet.server.codec.http.HTTPRequestMessage

class PelletHTTPRouter : HTTPRouting {

    private val logger = pelletLogger<PelletHTTPRouter>()
    private val internalRoutes = mutableMapOf<String, MutableList<PelletHTTPRoute>>()
    override val routes
        get() = internalRoutes.values.flatten()

    override fun add(route: PelletHTTPRoute) {
        val method = route.method.rawMethod
        if (!internalRoutes.containsKey(method)) {
            internalRoutes += method to mutableListOf(route)
        } else {
            internalRoutes[method]!! += route
        }
    }

    override fun route(
        message: HTTPRequestMessage
    ): HTTPRouting.ResolvedRoute? {
        val incomingRoutePath = PelletHTTPRoutePath.parse(message.requestLine.resourceUri.path)
        val validRoutes = internalRoutes[message.requestLine.method.rawMethod]
            ?: return null
        return resolve(incomingRoutePath, validRoutes)
    }

    private fun resolve(
        incomingRoutePath: PelletHTTPRoutePath,
        candidates: List<PelletHTTPRoute>
    ): HTTPRouting.ResolvedRoute? {
        val size = incomingRoutePath.components.size
        val incomingComponents = incomingRoutePath.components.map {
            if (it !is PelletHTTPRoutePath.Component.Plain) {
                return null
            }
            return@map it
        }
        val matchedRoutes = candidates
            .filter { it.routePath.components.size == size }
            .filter { candidate ->
                candidate.routePath.components.forEachIndexed { index, component ->
                    if (component is PelletHTTPRoutePath.Component.Plain) {
                        // todo: figure out URL case sensitivity
                        val matches = component.string.equals(
                            incomingComponents[index].string,
                            ignoreCase = true
                        )
                        if (!matches) {
                            return@filter false
                        }
                        return@forEachIndexed
                    }
                    if (component !is PelletHTTPRoutePath.Component.Variable) {
                        return@filter false
                    }
                }
                return@filter true
            }

        if (matchedRoutes.size > 1) {
            logger.warn { "multiple routes matched an incoming request" }
        }
        val matchedRoute = matchedRoutes.firstOrNull()
            ?: return null
        val valueMap = mutableMapOf<String, String>()
        matchedRoute.routePath.components.forEachIndexed { index, component ->
            if (component is PelletHTTPRoutePath.Component.Variable) {
                valueMap += component.name to incomingComponents[index].string
            }
        }
        return HTTPRouting.ResolvedRoute(
            matchedRoute,
            valueMap
        )
    }
}
