package dev.pellet.routing.http

import dev.pellet.codec.http.HTTPRequestMessage

interface HTTPRouting {

    val routes: List<PelletHTTPRoute>

    fun add(route: PelletHTTPRoute)
    fun route(message: HTTPRequestMessage): PelletHTTPRoute?
}
