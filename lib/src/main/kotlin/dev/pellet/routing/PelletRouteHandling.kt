package dev.pellet.routing

fun interface PelletRouteHandling<ContextType, ResponseType> {

    suspend fun handle(
        context: ContextType
    ): ResponseType
}
