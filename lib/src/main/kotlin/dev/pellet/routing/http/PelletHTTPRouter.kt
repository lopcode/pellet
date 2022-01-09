package dev.pellet.routing.http

import dev.pellet.codec.http.HTTPRequestMessage

class PelletHTTPRouter : HTTPRouting {

    private val routes = mutableListOf<PelletHTTPRoute>()

    override fun add(route: PelletHTTPRoute) {
        routes.add(route)
    }

    override fun route(
        message: HTTPRequestMessage
    ): PelletHTTPRoute? {
        // todo: investigate if route matching can be better than O(n)
        return routes.firstOrNull {
            it.method == message.requestLine.method && message.requestLine.resourceUri == it.uri
        }
    }
}
