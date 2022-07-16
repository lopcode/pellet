package dev.pellet.server.nio

import java.nio.channels.SocketChannel

data class NIOSocket(
    val channel: SocketChannel
)
