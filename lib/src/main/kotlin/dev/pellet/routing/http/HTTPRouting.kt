package dev.pellet.routing.http

import dev.pellet.codec.http.HTTPRequestMessage

interface HTTPRouting {

    fun add(route: PelletHTTPRoute)
    fun route(message: HTTPRequestMessage): PelletHTTPRoute?
}
