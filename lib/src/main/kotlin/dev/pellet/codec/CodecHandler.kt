package dev.pellet.codec

interface CodecHandler<T : Any> {

    suspend fun handle(output: T)
}
