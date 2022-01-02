package dev.pellet.routing.http

import dev.pellet.responder.http.PelletHTTPRouteContext
import dev.pellet.routing.PelletRouteHandling

fun interface PelletHTTPRouteHandling : PelletRouteHandling<PelletHTTPRouteContext, HTTPRouteResponse>
