package dev.pellet.responder.http

import dev.pellet.PelletClient
import dev.pellet.codec.http.HTTPRequestMessage

data class PelletHTTPRouteContext(
    val rawMessage: HTTPRequestMessage,
    internal val client: PelletClient
)
