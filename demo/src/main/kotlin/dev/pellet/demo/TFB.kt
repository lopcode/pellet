package dev.pellet.demo

import dev.pellet.logging.pelletLogger
import dev.pellet.server.PelletBuilder.httpRouter
import dev.pellet.server.PelletBuilder.pelletServer
import dev.pellet.server.PelletConnector
import dev.pellet.server.codec.mime.MediaType
import dev.pellet.server.responder.http.PelletHTTPRouteContext
import dev.pellet.server.routing.http.HTTPRouteResponse
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object TFB

private val logger = pelletLogger<Demo>()

fun main() = runBlocking {
    val sharedRouter = httpRouter {
        get("/plain", ::handlePlainBenchmark)
        get("/json", ::handleJSONBenchmark)
    }
    val pellet = pelletServer {
        logRequests = false
        httpConnector {
            endpoint = PelletConnector.Endpoint(
                hostname = "localhost",
                port = 8082
            )
            router = sharedRouter
        }
    }
    pellet.start()
}

private val dateFormatter = DateTimeFormatter
    .ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
    .withZone(ZoneId.of("GMT"))

private suspend fun handlePlainBenchmark(
    context: PelletHTTPRouteContext
): HTTPRouteResponse {
    return HTTPRouteResponse.Builder()
        .statusCode(200)
        .entity("Hello, World!", MediaType("text", "plain"))
        .header("Server", "pellet")
        .header("Date", dateFormatter.format(Instant.now()))
        .build()
}

@kotlinx.serialization.Serializable
private data class JSONResponseBody(
    val message: String
)

private suspend fun handleJSONBenchmark(
    context: PelletHTTPRouteContext
): HTTPRouteResponse {
    val responseBody = JSONResponseBody(message = "Hello, World!")
    return HTTPRouteResponse.Builder()
        .statusCode(200)
        .jsonEntity(Json, responseBody)
        .header("Server", "pellet")
        .header("Date", dateFormatter.format(Instant.now()))
        .build()
}
