package dev.pellet.server

import dev.pellet.server.codec.Codec
import dev.pellet.server.codec.MockCodec
import kotlinx.io.Buffer

class MockPelletServerClient : PelletServerClient {

    override var remoteHostString: String = ""
    override var codec: Codec = MockCodec()

    override fun write(vararg buffers: Buffer): Result<Long> {
        TODO("Not yet implemented")
    }

    override fun write(buffer: Buffer): Result<Long> {
        TODO("Not yet implemented")
    }

    override fun close(reason: CloseReason): Result<Unit> {
        TODO("Not yet implemented")
    }

    override fun read(buffer: Buffer): Result<Long> {
        TODO("Not yet implemented")
    }
}
