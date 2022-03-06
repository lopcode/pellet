package dev.pellet.logging

public class PelletLogElements(
    startingElements: Map<String, PelletLogElement> = mapOf()
) {
    private val elements = startingElements.toMutableMap()

    fun add(key: String, element: PelletLogElement): PelletLogElements {
        elements += key to element
        return this
    }

    fun all(): Map<String, PelletLogElement> {
        return elements
    }
}
