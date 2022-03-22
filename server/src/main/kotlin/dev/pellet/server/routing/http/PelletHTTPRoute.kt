package dev.pellet.server.routing.http

import dev.pellet.server.codec.http.HTTPMethod

data class PelletHTTPRoute(
    val method: HTTPMethod,
    internal val routePath: PelletHTTPRoutePath,
    val handler: PelletHTTPRouteHandling
) {

    val path: String
        get() = routePath.path
}
