package dev.pellet.server

import dev.pellet.server.codec.Codec
import kotlinx.io.Buffer

interface PelletServerClient {

    val remoteHostString: String
    val codec: Codec

    fun write(vararg buffers: Buffer): Result<Long>
    fun write(buffer: Buffer): Result<Long>
    fun close(reason: CloseReason): Result<Unit>
    fun read(buffer: Buffer): Result<Long>
}
