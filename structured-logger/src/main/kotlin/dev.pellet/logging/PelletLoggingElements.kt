package dev.pellet.logging

import kotlinx.serialization.json.JsonPrimitive

internal typealias PelletLoggingElement = Pair<String, JsonPrimitive>

internal class PelletLoggingElements(
    startingElements: Map<String, JsonPrimitive> = mapOf()
) {
    private val elements = startingElements.toMutableMap()

    fun all(): List<PelletLoggingElement> {
        return elements.toList()
    }
}
