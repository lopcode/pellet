package dev.pellet.server.codec

interface CodecHandler<T : Any> {

    suspend fun handle(output: T)
}
