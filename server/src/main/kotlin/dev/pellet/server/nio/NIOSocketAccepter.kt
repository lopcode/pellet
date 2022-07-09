package dev.pellet.server.nio

import dev.pellet.server.PelletServerClient
import dev.pellet.server.buffer.PelletBufferPooling
import dev.pellet.server.codec.Codec
import kotlinx.coroutines.runInterruptible
import java.net.SocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.SelectionKey.OP_ACCEPT
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel

internal class NIOSocketAccepter(
    private val pool: PelletBufferPooling
) {

    private val acceptSelector = Selector.open()

    suspend fun run(
        socketAddress: SocketAddress,
        acceptChannelSelector: (SocketChannel) -> Selector,
        codecFactory: () -> Codec
    ) {
        val serverSocketChannel = ServerSocketChannel.open()
        serverSocketChannel.bind(socketAddress, 8192)
        serverSocketChannel.configureBlocking(false)
        serverSocketChannel.socket().reuseAddress = true
        serverSocketChannel.register(acceptSelector, OP_ACCEPT)
        while (!Thread.currentThread().isInterrupted) {
            val numberSelected = runInterruptible {
                acceptSelector.select()
            }
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
                val socketChannel = runInterruptible {
                    serverChannel.accept()
                }
                socketChannel.socket().tcpNoDelay = true
                socketChannel.socket().reuseAddress = true
                socketChannel.configureBlocking(false)
                val socketSelector = acceptChannelSelector(socketChannel)
                val selectorKey = socketChannel.register(socketSelector, SelectionKey.OP_READ)
                val nioSocket = NIOSocket(socketChannel, codecFactory())
                val client = PelletServerClient(nioSocket, pool)
                selectorKey.attach(client)
                socketSelector.wakeup()
            }
        }
    }
}
