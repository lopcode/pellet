package dev.pellet.demo

import io.jooby.StatusCode
import io.jooby.runApp

fun main(args: Array<String>) {
    runApp(args) {
        serverOptions {
            port = 8089
        }
        get("/") { context -> context.send(StatusCode.NO_CONTENT) }
    }
}
