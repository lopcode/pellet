package dev.pellet.server.nio

import dev.pellet.server.codec.Codec
import java.nio.channels.SocketChannel

data class NIOSocket(
    val channel: SocketChannel,
    val codec: Codec
)
