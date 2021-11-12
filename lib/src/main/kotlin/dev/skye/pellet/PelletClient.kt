package dev.skye.pellet

import dev.skye.pellet.codec.Codec
import java.nio.channels.AsynchronousSocketChannel

class PelletClient(
    val socket: AsynchronousSocketChannel
) {
    lateinit var codec: Codec
}
