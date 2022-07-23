package dev.pellet.server.routing.http

import dev.pellet.server.responder.http.PelletHTTPRouteContext

class MockHTTPRouteHandler : PelletHTTPRouteHandling {

    override suspend fun handle(
        context: PelletHTTPRouteContext
    ): HTTPRouteResponse {
        TODO("not stubbed")
    }
}
