package dev.pellet.server.codec.http

import dev.pellet.logging.pelletLogger
import dev.pellet.server.CloseReason
import dev.pellet.server.PelletServerClient
import dev.pellet.server.buffer.PelletBufferPooling
import dev.pellet.server.codec.CodecHandler
import dev.pellet.server.metrics.PelletTimer
import dev.pellet.server.responder.http.PelletHTTPResponder
import dev.pellet.server.responder.http.PelletHTTPRouteContext
import dev.pellet.server.routing.http.HTTPRouteResponse
import dev.pellet.server.routing.http.HTTPRouting

internal class HTTPRequestHandler(
    private val client: PelletServerClient,
    private val router: HTTPRouting,
    private val pool: PelletBufferPooling
) : CodecHandler<HTTPRequestMessage> {

    private val timer = PelletTimer()
    private val logger = pelletLogger<HTTPRequestHandler>()

    override suspend fun handle(output: HTTPRequestMessage) {
        timer.reset()
        val context = PelletHTTPRouteContext(output, client)
        val responder = PelletHTTPResponder(client, pool)
        val route = router.route(output)
        if (route == null) {
            val response = HTTPRouteResponse.Builder()
                .notFound()
                .build()
            val message = mapRouteResponseToMessage(response)
            responder.respond(message)
        } else {
            val routeResult = runCatching {
                route.handler.handle(context)
            }
            if (routeResult.isFailure) {
                logger.error("failed to handle request", routeResult.exceptionOrNull())
                val response = HTTPRouteResponse.Builder()
                    .internalServerError()
                    .build()
                val message = mapRouteResponseToMessage(response)
                responder.respond(message)
            }
            val message = mapRouteResponseToMessage(routeResult.getOrThrow())
            responder.respond(message)
        }

        val connectionHeader = output.headers.getSingleOrNull(HTTPHeaderConstants.connection)
        handleConnectionHeader(connectionHeader)
        output.release(pool)

        // todo: track request durations
        val requestDuration = timer.markAndReset()
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

    private fun mapRouteResponseToMessage(
        routeResult: HTTPRouteResponse
    ): HTTPResponseMessage {
        return HTTPResponseMessage(
            statusLine = HTTPStatusLine(
                version = "HTTP/1.1",
                statusCode = routeResult.statusCode,
                reasonPhrase = mapCodeToReasonPhrase(routeResult.statusCode)
            ),
            headers = routeResult.headers,
            entity = routeResult.entity
        )
    }

    private fun mapCodeToReasonPhrase(code: Int) = when (code) {
        200 -> "OK"
        204 -> "No Content"
        404 -> "Not Found"
        500 -> "Internal Server Error"
        else -> "Unknown"
    }
}
