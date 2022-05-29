package dev.pellet.server.codec.http

import dev.pellet.server.codec.CodecHandler

class MockCodecHandler<T : Any> : CodecHandler<T> {

    val spyHandleOutput = mutableListOf<T>()

    override suspend fun handle(output: T) {
        spyHandleOutput.add(output)
    }
}
