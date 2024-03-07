package dev.pellet.server.socket

import dev.pellet.logging.pelletLogger
import dev.pellet.server.CloseReason
import kotlinx.io.Buffer

class BlockingSocketHandler {

    private val logger = pelletLogger<BlockingSocketHandler>()

    fun handle(client: BlockingPelletServerClient) {
        val buffer = Buffer()
        while (!Thread.currentThread().isInterrupted) {
            val bytesRead = client.read(buffer).getOrElse {
                client.close(CloseReason.ServerException(it))
                return
            }
            if (bytesRead < 0) {
                // socket closed - client initiated
                client.close(CloseReason.ClientInitiated)
                return
            }

            // todo: evaluate if runcatching is necessary here
            runCatching {
                client.codec.consume(buffer, client)
            }.getOrElse {
                logger.error(it) { "codec failure" }
                return
            }
            buffer.clear()
        }
    }
}
