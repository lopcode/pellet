package dev.skye.pellet

import dev.skye.pellet.codec.http.HTTPRequestMessage

data class PelletContext(
    val message: HTTPRequestMessage,
    internal val client: PelletClient
)
