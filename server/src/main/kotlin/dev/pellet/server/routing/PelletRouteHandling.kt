package dev.pellet.server.routing

fun interface PelletRouteHandling<ContextType, ResponseType> {

    suspend fun handle(
        context: ContextType
    ): ResponseType
}
