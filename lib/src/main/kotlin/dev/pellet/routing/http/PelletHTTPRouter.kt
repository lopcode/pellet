package dev.pellet.routing.http

import dev.pellet.codec.http.HTTPRequestMessage

class PelletHTTPRouter(
    private val routes: List<HTTPRoute>
) : HTTPRouting {

    override fun route(
        message: HTTPRequestMessage
    ): HTTPRoute? {
        // todo: evaluate route paths properly and cache result
        return routes.firstOrNull {
            it.method == message.requestLine.method && it.path == message.requestLine.resourceUri
        }
    }
}
