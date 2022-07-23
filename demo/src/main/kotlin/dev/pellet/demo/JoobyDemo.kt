package dev.pellet.demo

import io.jooby.MediaType
import io.jooby.StatusCode
import io.jooby.runApp
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@kotlinx.serialization.Serializable
private data class JoobyResponseBody(
    val message: String
)

fun main(args: Array<String>) {
    runApp(args) {
        serverOptions {
            port = 8089
        }
        get("/") { context -> context.send(StatusCode.NO_CONTENT) }
        get("/v1/hello") { context ->
            val response = JoobyResponseBody(message = "hello, world 🌎")
            val responseBody = Json.encodeToString(response)
            context.responseType = MediaType.json
            responseBody
        }
    }
}
