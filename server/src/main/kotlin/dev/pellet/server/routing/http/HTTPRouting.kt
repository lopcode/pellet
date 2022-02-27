package dev.pellet.server.routing.http

import dev.pellet.server.codec.http.HTTPRequestMessage

interface HTTPRouting {

    val routes: List<PelletHTTPRoute>

    fun add(route: PelletHTTPRoute)
    fun route(message: HTTPRequestMessage): PelletHTTPRoute?
}
