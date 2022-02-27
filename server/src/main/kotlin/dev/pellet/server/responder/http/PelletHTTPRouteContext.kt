package dev.pellet.server.responder.http

import dev.pellet.server.PelletServerClient
import dev.pellet.server.codec.http.HTTPRequestMessage

data class PelletHTTPRouteContext(
    val rawMessage: HTTPRequestMessage,
    internal val client: PelletServerClient
)
