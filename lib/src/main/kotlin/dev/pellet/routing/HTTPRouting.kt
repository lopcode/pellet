package dev.pellet.routing

import dev.pellet.codec.http.HTTPRequestMessage

interface HTTPRouting {

    fun route(message: HTTPRequestMessage): HTTPRoute?
}
