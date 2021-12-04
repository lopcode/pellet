package dev.pellet.codec

import java.nio.ByteBuffer

/**
 * A codec maintains internal state, such that it can interpret a bag of bytes given to it, and output objects to a
 * [CodecHandler] instance.
 *
 * For example, reading HTTP 1.1 requires a codec to keep track of whether you're reading the request line, headers,
 * a fixed or streaming entity, etc.
*/
interface Codec {

    fun clear()
    suspend fun consume(bytes: ByteBuffer)
}
