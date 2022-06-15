package dev.pellet.server.codec.http

import dev.pellet.server.PelletServerClient

internal data class IncomingMessageWorkItem(
    val message: HTTPRequestMessage,
    val client: PelletServerClient
)
