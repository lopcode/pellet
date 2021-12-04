package dev.pellet

import dev.pellet.codec.http.HTTPRequestMessage

data class PelletContext(
    val message: HTTPRequestMessage,
    internal val client: PelletClient
)
