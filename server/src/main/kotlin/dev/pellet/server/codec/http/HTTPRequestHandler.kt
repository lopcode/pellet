package dev.pellet.server.codec.http

import dev.pellet.logging.PelletLogElements
import dev.pellet.logging.logElement
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
import java.time.Instant
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatterBuilder
import java.time.format.TextStyle
import java.time.temporal.ChronoField

internal class HTTPRequestHandler(
    private val client: PelletServerClient,
    private val router: HTTPRouting,
    private val pool: PelletBufferPooling,
    private val logRequests: Boolean
) : CodecHandler<HTTPRequestMessage> {

    private val timer = PelletTimer()
    private val logger = pelletLogger<HTTPRequestHandler>()

    companion object {

        val commonDateFormat = DateTimeFormatterBuilder()
            .appendValue(ChronoField.DAY_OF_MONTH, 2)
            .appendLiteral('/')
            .appendText(ChronoField.MONTH_OF_YEAR, TextStyle.SHORT)
            .appendLiteral('/')
            .appendValue(ChronoField.YEAR, 4)
            .appendLiteral(':')
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .appendLiteral(' ')
            .appendOffset("+HHMM", "0000")
            .toFormatter()!!

        const val requestMethodKey = "request.method"
        const val requestUriKey = "request.uri"
        const val responseCodeKey = "response.code"
        const val responseDurationKey = "response.duration_ms"
    }

    override suspend fun handle(output: HTTPRequestMessage) {
        timer.reset()
        val context = PelletHTTPRouteContext(output, client)
        val responder = PelletHTTPResponder(client, pool)
        val route = router.route(output)
        if (route == null) {
            val response = HTTPRouteResponse.Builder()
                .notFound()
                .build()
            respond(output, response, responder, timer)
        } else {
            val routeResult = runCatching {
                route.handler.handle(context)
            }
            if (routeResult.isFailure) {
                logger.error(routeResult.exceptionOrNull()) { "failed to handle request" }
                val response = HTTPRouteResponse.Builder()
                    .internalServerError()
                    .build()
                respond(output, response, responder, timer)
            } else {
                respond(output, routeResult.getOrThrow(), responder, timer)
            }
        }

        val connectionHeader = output.headers.getSingleOrNull(HTTPHeaderConstants.connection)
        handleConnectionHeader(connectionHeader)
        output.release(pool)
    }

    private suspend fun respond(
        request: HTTPRequestMessage,
        response: HTTPRouteResponse,
        responder: PelletHTTPResponder,
        timer: PelletTimer
    ) {
        val message = mapRouteResponseToMessage(response)
        val requestDuration = timer.markAndReset()
        if (logRequests) {
            val elements = PelletLogElements(
                mapOf(
                    requestMethodKey to logElement(request.requestLine.method.toString()),
                    requestUriKey to logElement(request.requestLine.resourceUri.toString()),
                    responseCodeKey to logElement(message.statusLine.statusCode),
                    responseDurationKey to logElement(requestDuration.toMillis())
                )
            )

            logRequestResponse(request, response, elements)
        }
        responder.respond(message)
    }

    private fun logRequestResponse(
        request: HTTPRequestMessage,
        response: HTTPRouteResponse,
        elements: PelletLogElements
    ) {
        val dateTime = commonDateFormat.format(Instant.now().atZone(UTC))
        val (method, uri, version) = request.requestLine
        val responseSize = response.entity.sizeBytes
        logger.info(elements) { "${client.remoteHostString} - - [$dateTime] \"$method $uri $version\" ${response.statusCode} $responseSize" }
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
