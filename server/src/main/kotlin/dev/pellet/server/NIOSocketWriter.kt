package dev.pellet.server

import dev.pellet.server.buffer.PelletBuffer
import java.util.concurrent.BlockingQueue

data class WriteItem(
    val buffer: PelletBuffer,
    val client: PelletServerClient
)

internal class NIOSocketWriter() {

    fun run(
        writeChannel: BlockingQueue<WriteItem>
    ) {
        while (!Thread.currentThread().isInterrupted) {
            val writeItem = writeChannel.take()
            writeItem.client.writeAndRelease(writeItem.buffer)
        }
    }
}
