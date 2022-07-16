package dev.pellet.server

import dev.pellet.server.buffer.PelletBuffer
import dev.pellet.server.codec.Codec

interface PelletServerClient {

    val remoteHostString: String
    val codec: Codec

    fun writeAndRelease(vararg buffer: PelletBuffer): Result<Long>
    fun writeAndRelease(buffer: PelletBuffer): Result<Int>
    fun close(reason: CloseReason): Result<Unit>
    fun read(buffer: PelletBuffer): Result<Int>
}
