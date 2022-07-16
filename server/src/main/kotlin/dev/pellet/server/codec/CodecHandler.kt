package dev.pellet.server.codec

import dev.pellet.server.PelletServerClient

interface CodecHandler<T : Any> {

    suspend fun handle(
        output: T,
        client: PelletServerClient
    )
}
