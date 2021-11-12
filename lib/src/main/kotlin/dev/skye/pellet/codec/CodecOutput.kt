package dev.skye.pellet.codec

interface CodecOutput<T : Any> {

    suspend fun output(thing: T)
}
