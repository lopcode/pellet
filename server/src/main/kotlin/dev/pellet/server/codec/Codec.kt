package dev.pellet.server.codec

import dev.pellet.server.PelletServerClient
import kotlinx.io.Buffer

/**
 * A codec maintains internal state, such that it can interpret a bag of bytes given to it, and output objects to a
 * [CodecHandler] instance.
 *
 * For example, reading HTTP 1.1 requires a codec to keep track of whether you're reading the request line, headers,
 * a fixed or streaming entity, etc.
*/
interface Codec {

    fun clear()

    /**
     * Process bytes stored in the buffer, such that it can be safely recycled
     */
    fun consume(
        buffer: Buffer,
        client: PelletServerClient
    )
}
