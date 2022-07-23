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
        val incomingComponents = PelletHTTPRoutePath.parsePlainComponents(message.requestLine.resourceUri.path)
        val validRoutes = internalRoutes[message.requestLine.method.rawMethod]
            ?: return null
        return resolve(incomingComponents, validRoutes)
    }

    private fun resolve(
        incomingComponents: List<PelletHTTPRoutePath.Component.Plain>,
        candidates: List<PelletHTTPRoute>
    ): HTTPRouting.ResolvedRoute? {
        val size = incomingComponents.size
        val matchedRoute = candidates
            .filter { it.routePath.components.size == size }
            .firstOrNull { candidate ->
                candidate.routePath.components.forEachIndexed { index, component ->
                    when (component) {
                        is PelletHTTPRoutePath.Component.Plain -> {
                            // todo: figure out URL case sensitivity
                            val matches = component.string.equals(
                                incomingComponents[index].string,
                                ignoreCase = true
                            )
                            if (!matches) {
                                return@firstOrNull false
                            }
                            return@forEachIndexed
                        }
                        is PelletHTTPRoutePath.Component.Variable -> {
                            // permitted - continue
                        }
                    }
                }
                return@firstOrNull true
            }
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
