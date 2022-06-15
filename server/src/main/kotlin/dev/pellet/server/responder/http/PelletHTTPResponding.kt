package dev.pellet.server.responder.http

import dev.pellet.server.codec.http.HTTPResponseMessage

interface PelletHTTPResponding {

    fun respond(message: HTTPResponseMessage): Result<Unit>
}
