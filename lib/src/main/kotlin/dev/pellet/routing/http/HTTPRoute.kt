package dev.pellet.routing.http

import dev.pellet.codec.http.HTTPMethod

data class HTTPRoute(
    val method: HTTPMethod,
    val path: String,
    val handler: PelletHTTPRouteHandling
)
