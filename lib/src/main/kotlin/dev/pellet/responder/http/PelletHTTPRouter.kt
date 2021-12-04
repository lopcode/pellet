package dev.pellet.responder.http

import dev.pellet.codec.http.HTTPRequestMessage

class PelletHTTPRouter(
    private val routes: List<HTTPRoute>
) : HTTPRouting {

    override fun route(
        message: HTTPRequestMessage
    ): HTTPRoute? {
        // todo: evaluate route paths properly and cache result
        return routes.firstOrNull {
            it.path == message.requestLine.resourceUri
        }
    }
}
