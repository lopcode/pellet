package dev.pellet.server

import dev.pellet.server.buffer.PelletBuffer
import dev.pellet.server.codec.Codec
import dev.pellet.server.codec.MockCodec

class MockPelletServerClient : PelletServerClient {

    override var remoteHostString: String = ""
    override var codec: Codec = MockCodec()

    override fun writeAndRelease(vararg buffer: PelletBuffer): Result<Long> {
        TODO("Not yet implemented")
    }

    override fun writeAndRelease(buffer: PelletBuffer): Result<Int> {
        TODO("Not yet implemented")
    }

    override fun close(reason: CloseReason): Result<Unit> {
        TODO("Not yet implemented")
    }

    override fun read(buffer: PelletBuffer): Result<Int> {
        TODO("Not yet implemented")
    }
}
