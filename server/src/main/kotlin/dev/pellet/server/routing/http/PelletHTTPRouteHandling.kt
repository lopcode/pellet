package dev.pellet.server.routing.http

import dev.pellet.server.responder.http.PelletHTTPRouteContext
import dev.pellet.server.routing.PelletRouteHandling

fun interface PelletHTTPRouteHandling : PelletRouteHandling<PelletHTTPRouteContext, HTTPRouteResponse>
