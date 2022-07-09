package dev.pellet.server.nio

import dev.pellet.server.codec.Codec
import java.nio.channels.SelectionKey
import java.nio.channels.SelectionKey.OP_READ
import java.nio.channels.SelectionKey.OP_WRITE
import java.nio.channels.Selector
import java.nio.channels.SocketChannel

data class NIOSocket(
    val channel: SocketChannel,
    val codec: Codec,
    val key: SelectionKey,
    val selector: Selector
) {

    internal fun markReadWrite() {
        key.interestOpsOr(OP_WRITE)
        selector.wakeup()
    }

    internal fun markReadOnly() {
        key.interestOps(OP_READ)
    }
}
