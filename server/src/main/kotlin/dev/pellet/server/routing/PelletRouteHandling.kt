package dev.pellet.server.routing

fun interface PelletRouteHandling<ContextType, ResponseType> {

    fun handle(
        context: ContextType
    ): ResponseType
}
