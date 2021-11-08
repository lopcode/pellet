package dev.skye.pellet

import io.jooby.StatusCode
import io.jooby.runApp

fun main(args: Array<String>) {
    runApp(args) {
        get("/") { context -> context.send(StatusCode.NO_CONTENT) }
    }
}
