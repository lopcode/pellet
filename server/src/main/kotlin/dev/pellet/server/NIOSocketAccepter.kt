package dev.pellet.server

import dev.pellet.server.buffer.PelletBufferPooling
import dev.pellet.server.codec.Codec
import java.net.SocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.SelectionKey.OP_ACCEPT
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel

class NIOSocketAccepter(
    private val pool: PelletBufferPooling
) {

    private val acceptSelector = Selector.open()

    fun run(
        socketAddress: SocketAddress,
        acceptChannelSelector: (SocketChannel) -> Selector,
        codecFactory: () -> Codec
    ) {
        val serverSocketChannel = ServerSocketChannel.open()
        serverSocketChannel.bind(socketAddress, 8192)
        serverSocketChannel.configureBlocking(false)
        serverSocketChannel.register(acceptSelector, OP_ACCEPT)
        while (!Thread.currentThread().isInterrupted) {
            val numberSelected = acceptSelector.select()
            if (numberSelected <= 0) {
                continue
            }
            val selectedKeys = acceptSelector.selectedKeys()
            val keysIterator = selectedKeys.iterator()
            while (keysIterator.hasNext()) {
                val key = keysIterator.next()
                keysIterator.remove()
                if (!key.isAcceptable) {
                    continue
                }
                val serverChannel = key.channel() as ServerSocketChannel
                val socketChannel = serverChannel.accept()
                socketChannel.configureBlocking(false)
                val readSelector = acceptChannelSelector(socketChannel)
                val nioSocket = NIOSocket(socketChannel, codecFactory())
                val client = PelletServerClient(nioSocket, pool)
                val selectorKey = socketChannel.register(readSelector, SelectionKey.OP_READ)
                selectorKey.attach(client)
                readSelector.wakeup()
            }
        }
    }
}
