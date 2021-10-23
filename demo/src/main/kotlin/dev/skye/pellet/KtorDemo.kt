package dev.skye.pellet

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    embeddedServer(Netty, port = 8082) {
        routing {
            get("/") {
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }.start(wait = true)
}
