package dev.pellet.routing.http

import dev.pellet.codec.http.HTTPMethod
import java.net.URI

data class PelletHTTPRoute(
    val method: HTTPMethod,
    internal val uri: URI,
    val handler: PelletHTTPRouteHandling
) {

    val scheme: String?
        get() = uri.scheme

    val host: String?
        get() = uri.host

    val port: Int?
        get() = when {
            uri.port > 0 -> uri.port
            else -> null
        }

    val path: String
        get() = uri.path

    val query: String?
        get() = uri.query
}
