package dev.pellet.handler

import dev.pellet.responder.http.PelletHTTPContext
import dev.pellet.responder.http.PelletHTTPResponder

fun interface PelletHTTPRouteHandling {

    suspend fun handle(
        context: PelletHTTPContext,
        responder: PelletHTTPResponder
    )
}
