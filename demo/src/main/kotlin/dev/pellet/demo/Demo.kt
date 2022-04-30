package dev.pellet.demo

import dev.pellet.logging.pelletLogger
import dev.pellet.server.PelletBuilder.httpRouter
import dev.pellet.server.PelletBuilder.pelletServer
import dev.pellet.server.PelletConnector
import dev.pellet.server.responder.http.PelletHTTPRouteContext
import dev.pellet.server.routing.http.HTTPRouteResponse
import dev.pellet.server.routing.http.PelletHTTPRoutePath
import dev.pellet.server.routing.stringDescriptor
import dev.pellet.server.routing.uuidDescriptor
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

object Demo

val logger = pelletLogger<Demo>()
val idDescriptor = uuidDescriptor("id")
val suffixDescriptor = stringDescriptor("suffix")

fun main() = runBlocking {
    val helloIdPath = PelletHTTPRoutePath.Builder()
        .addComponents("/v1")
        .addVariable(idDescriptor)
        .addComponents("/hello")
        .build()
    val sharedRouter = httpRouter {
        get("/", ::handleRequest)
        get("/v1/hello", ::handleResponseBody)
        get(helloIdPath, ::handleNamedResponseBody)
        post("/v1/echo", ::handleEchoRequest)
        get("/v1/error", ::handleForceError)
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
        httpConnector {
            endpoint = PelletConnector.Endpoint(
                hostname = "localhost",
                port = 8083
            )
            router = sharedRouter
        }
    }
    pellet.start().join()
}

fun simpleMain() = runBlocking {
    val pellet = pelletServer {
        httpConnector {
            endpoint = PelletConnector.Endpoint(
                hostname = "localhost",
                port = 8082
            )
            router {
                get("/v1/hello") {
                    HTTPRouteResponse.Builder()
                        .noContent()
                        .header("X-Hello", "World")
                        .build()
                }
            }
        }
    }
    pellet.start().join()
}

private suspend fun handleRequest(
    context: PelletHTTPRouteContext
): HTTPRouteResponse {
    logger.debug { "got request: ${context.rawMessage}" }

    return HTTPRouteResponse.Builder()
        .noContent()
        .header("X-Hello", "World")
        .build()
}

@kotlinx.serialization.Serializable
data class RequestBody(
    val message: String
)

@kotlinx.serialization.Serializable
data class ResponseBody(
    val message: String
)

private suspend fun handleEchoRequest(
    context: PelletHTTPRouteContext
): HTTPRouteResponse {
    logger.debug { "got echo POST request: ${context.rawMessage}" }

    val requestBody = context.decodeRequestBody<RequestBody>(Json).getOrElse {
        logger.debug(it) { "failed to decode json body" }

        return HTTPRouteResponse.Builder()
            .badRequest()
            .build()
    }
    val responseBody = ResponseBody(
        message = requestBody.message
    )
    return HTTPRouteResponse.Builder()
        .jsonEntity(Json, responseBody)
        .build()
}

private suspend fun handleForceError(
    context: PelletHTTPRouteContext
): HTTPRouteResponse {
    throw RuntimeException("intentional error")
}

private suspend fun handleResponseBody(
    context: PelletHTTPRouteContext
): HTTPRouteResponse {
    val responseBody = ResponseBody(message = "hello, world 🌎")
    return HTTPRouteResponse.Builder()
        .statusCode(200)
        .jsonEntity(Json, responseBody)
        .header("X-Hello", "World")
        .build()
}

private suspend fun handleNamedResponseBody(
    context: PelletHTTPRouteContext
): HTTPRouteResponse {
    val id = context.pathParameter(idDescriptor).getOrThrow()
    val suffix = context.firstQueryParameter(suffixDescriptor).getOrNull()
        ?: "👋"
    val responseBody = ResponseBody(message = "hello $id $suffix")
    return HTTPRouteResponse.Builder()
        .statusCode(200)
        .jsonEntity(Json, responseBody)
        .build()
}
