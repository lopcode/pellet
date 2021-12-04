package dev.pellet.responder.http

import dev.pellet.PelletClient
import dev.pellet.codec.http.HTTPRequestMessage

data class PelletHTTPContext(
    val message: HTTPRequestMessage,
    internal val client: PelletClient
)
