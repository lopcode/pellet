package dev.pellet.routing

import dev.pellet.codec.http.HTTPMethod
import dev.pellet.handler.PelletHTTPRouteHandling

data class HTTPRoute(
    val method: HTTPMethod,
    val path: String,
    val handler: PelletHTTPRouteHandling
)
