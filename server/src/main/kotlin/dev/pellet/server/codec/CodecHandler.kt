package dev.pellet.server.codec

import dev.pellet.server.PelletServerClient

internal interface CodecHandler<T : Any> {

    suspend fun handle(
        output: T,
        client: PelletServerClient
    )
}
