package dev.skye.pellet.codec

interface CodecHandler<T : Any> {

    suspend fun handle(request: T)
}
