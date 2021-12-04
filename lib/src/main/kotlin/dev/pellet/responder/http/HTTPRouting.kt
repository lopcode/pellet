package dev.pellet.responder.http

import dev.pellet.codec.http.HTTPRequestMessage

interface HTTPRouting {

    fun route(message: HTTPRequestMessage): HTTPRoute?
}
