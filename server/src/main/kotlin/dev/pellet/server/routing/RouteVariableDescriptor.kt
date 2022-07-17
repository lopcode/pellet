package dev.pellet.server.routing

import java.util.UUID

public data class RouteVariableDescriptor<T : Any?>(
    val name: String,
    val visualType: String,
    val deserialiser: (String) -> T
)

public fun stringDescriptor(name: String): RouteVariableDescriptor<String> {
    return RouteVariableDescriptor(name, "string") { it }
}

public fun uuidDescriptor(name: String): RouteVariableDescriptor<UUID> {
    return RouteVariableDescriptor(name, "uuid") {
        UUID.fromString(it)
    }
}
