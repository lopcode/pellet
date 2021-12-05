package dev.pellet.codec.http

import dev.pellet.CloseReason
import dev.pellet.PelletClient
import dev.pellet.codec.CodecHandler
import dev.pellet.responder.http.PelletHTTPContext
import dev.pellet.responder.http.PelletHTTPResponder
import dev.pellet.routing.HTTPRouting

internal class HTTPRequestHandler(
    private val client: PelletClient,
    private val router: HTTPRouting
) : CodecHandler<HTTPRequestMessage> {

    override suspend fun handle(output: HTTPRequestMessage) {
        val context = PelletHTTPContext(output, client)
        val responder = PelletHTTPResponder(client)
        val route = router.route(output)
        if (route == null) {
            responder.writeNotFound()
        } else {
            route.handler.handle(context, responder)
        }

        val connectionHeader = output.headers.getSingleOrNull(HTTPHeaderConstants.connection)
        handleConnectionHeader(connectionHeader)
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
