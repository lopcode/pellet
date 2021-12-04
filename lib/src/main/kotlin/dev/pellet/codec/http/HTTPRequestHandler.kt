package dev.pellet.codec.http

import dev.pellet.CloseReason
import dev.pellet.PelletClient
import dev.pellet.PelletContext
import dev.pellet.PelletResponder
import dev.pellet.codec.CodecHandler

internal class HTTPRequestHandler(
    private val client: PelletClient,
    private val action: suspend (PelletContext, PelletResponder) -> Unit
) : CodecHandler<HTTPRequestMessage> {

    override suspend fun handle(output: HTTPRequestMessage) {
        val context = PelletContext(output, client)
        val responder = PelletResponder(client)
        action(context, responder)

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
