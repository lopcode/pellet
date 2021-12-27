package dev.pellet.codec.http

import dev.pellet.CloseReason
import dev.pellet.PelletClient
import dev.pellet.buffer.PelletBufferPooling
import dev.pellet.codec.CodecHandler
import dev.pellet.logging.logger
import dev.pellet.responder.http.PelletHTTPContext
import dev.pellet.responder.http.PelletHTTPResponder
import dev.pellet.routing.HTTPRouting

internal class HTTPRequestHandler(
    private val client: PelletClient,
    private val router: HTTPRouting,
    private val pool: PelletBufferPooling
) : CodecHandler<HTTPRequestMessage> {

    private val logger = logger<HTTPRequestHandler>()

    override suspend fun handle(output: HTTPRequestMessage) {
        val context = PelletHTTPContext(output, client)
        val responder = PelletHTTPResponder(client, pool)
        val route = router.route(output)
        if (route == null) {
            responder.writeNotFound()
        } else {
            val handled = runCatching {
                route.handler.handle(context, responder)
            }
            if (handled.isFailure) {
                logger.error("failed to handle request", handled.exceptionOrNull())
            }
        }

        val connectionHeader = output.headers.getSingleOrNull(HTTPHeaderConstants.connection)
        handleConnectionHeader(connectionHeader)

        output.release(pool)
    }

    private fun handleConnectionHeader(connectionHeader: HTTPHeader?) {
        if (connectionHeader == null) {
            // keep alive by default
            return
        }

        if (connectionHeader.rawValue.equals(HTTPHeaderConstants.keepAlive, ignoreCase = true)) {
            return
        }

        if (connectionHeader.rawValue.equals(HTTPHeaderConstants.close, ignoreCase = true)) {
            client.close(CloseReason.ServerInitiated)
        }
    }
}
