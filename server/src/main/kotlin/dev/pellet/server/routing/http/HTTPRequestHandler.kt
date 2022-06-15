package dev.pellet.server.routing.http

import dev.pellet.logging.PelletLogElements
import dev.pellet.logging.logElements
import dev.pellet.logging.pelletLogger
import dev.pellet.server.CloseReason
import dev.pellet.server.PelletServerClient
import dev.pellet.server.WriteItem
import dev.pellet.server.buffer.PelletBufferPooling
import dev.pellet.server.codec.CodecHandler
import dev.pellet.server.codec.http.ContentTypeParser
import dev.pellet.server.codec.http.HTTPEntity
import dev.pellet.server.codec.http.HTTPHeader
import dev.pellet.server.codec.http.HTTPHeaderConstants
import dev.pellet.server.codec.http.HTTPRequestMessage
import dev.pellet.server.codec.http.HTTPResponseMessage
import dev.pellet.server.codec.http.HTTPStatusLine
import dev.pellet.server.codec.http.query.QueryParser
import dev.pellet.server.metrics.PelletTimer
import dev.pellet.server.responder.http.PelletHTTPResponder
import dev.pellet.server.responder.http.PelletHTTPRouteContext
import java.time.Instant
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatterBuilder
import java.time.format.TextStyle
import java.time.temporal.ChronoField
import java.util.Queue

internal class HTTPRequestHandler(
    private val router: HTTPRouting,
    private val pool: PelletBufferPooling,
    private val writeQueue: Queue<WriteItem>,
    private val logRequests: Boolean,
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

    override suspend fun handle(
        output: HTTPRequestMessage,
        client: PelletServerClient
    ) {
        timer.reset()
        val responder = PelletHTTPResponder(writeQueue, client, pool)
        val resolvedRoute = router.route(output)
        if (resolvedRoute == null) {
            val response = HTTPRouteResponse.Builder()
                .notFound()
                .build()
            respond(output, response, responder, client, timer)
        } else {
            val response = handleRoute(resolvedRoute, output, client)
            respond(output, response, responder, client, timer)
        }

        val connectionHeader = output.headers.getSingleOrNull(HTTPHeaderConstants.connection)
        handleConnectionHeader(connectionHeader, client)
        output.release(pool)
    }

    private suspend fun handleRoute(
        resolvedRoute: HTTPRouting.ResolvedRoute,
        rawMessage: HTTPRequestMessage,
        client: PelletServerClient
    ): HTTPRouteResponse {
        val (route, valueMap) = resolvedRoute
        val query = rawMessage.requestLine.resourceUri.rawQuery ?: ""
        val queryParameters = QueryParser.parseEncodedQuery(query).getOrElse {
            return HTTPRouteResponse.Builder()
                .badRequest()
                .build()
        }
        val entityContext = when (rawMessage.entity) {
            is HTTPEntity.Content -> {
                val contentTypeHeader = rawMessage
                    .headers[HTTPHeaderConstants.contentType]
                    ?.rawValue
                if (contentTypeHeader == null) {
                    logger.debug { "got an entity but didn't receive a content type" }
                    return HTTPRouteResponse.Builder()
                        .badRequest()
                        .build()
                }
                val contentType = ContentTypeParser.parse(contentTypeHeader).getOrElse {
                    logger.debug { "received a malformed content type" }
                    return HTTPRouteResponse.Builder()
                        .badRequest()
                        .build()
                }
                PelletHTTPRouteContext.EntityContext(
                    rawEntity = rawMessage.entity,
                    contentType = contentType
                )
            }
            else -> null
        }
        val context = PelletHTTPRouteContext(rawMessage, entityContext, client, valueMap, queryParameters)
        val routeResult = runCatching {
            route.handler.handle(context)
        }
        return routeResult.getOrElse {
            logger.error(routeResult.exceptionOrNull()) { "failed to handle request" }
            HTTPRouteResponse.Builder()
                .internalServerError()
                .build()
        }
    }

    private fun respond(
        request: HTTPRequestMessage,
        response: HTTPRouteResponse,
        responder: PelletHTTPResponder,
        client: PelletServerClient,
        timer: PelletTimer
    ) {
        val message = mapRouteResponseToMessage(response)
        val requestDuration = timer.markAndReset()
        if (logRequests) {
            val elements = logElements {
                add(requestMethodKey, request.requestLine.method.toString())
                add(requestUriKey, request.requestLine.resourceUri.toString())
                add(responseCodeKey, message.statusLine.statusCode)
                add(responseDurationKey, requestDuration.toMillis())
            }
            logResponse(request, response, client, elements)
        }
        responder.respond(message)
    }

    private fun logResponse(
        request: HTTPRequestMessage,
        response: HTTPRouteResponse,
        client: PelletServerClient,
        elements: () -> PelletLogElements
    ) {
        val dateTime = commonDateFormat.format(Instant.now().atZone(UTC))
        val (method, uri, version) = request.requestLine
        val responseSize = response.entity.sizeBytes
        logger.info(elements) { "${client.remoteHostString} - - [$dateTime] \"$method $uri $version\" ${response.statusCode} $responseSize" }
    }

    private fun handleConnectionHeader(
        connectionHeader: HTTPHeader?,
        client: PelletServerClient
    ) {
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
        val effectiveStatusCode = when (routeResult.statusCode) {
            0 -> 200
            else -> routeResult.statusCode
        }
        return HTTPResponseMessage(
            statusLine = HTTPStatusLine(
                version = "HTTP/1.1",
                statusCode = effectiveStatusCode,
                reasonPhrase = mapCodeToReasonPhrase(effectiveStatusCode)
            ),
            headers = routeResult.headers,
            entity = routeResult.entity
        )
    }

    private fun mapCodeToReasonPhrase(code: Int) = when (code) {
        200 -> "OK"
        204 -> "No Content"
        400 -> "Bad Request"
        404 -> "Not Found"
        500 -> "Internal Server Error"
        else -> "Unknown"
    }
}
