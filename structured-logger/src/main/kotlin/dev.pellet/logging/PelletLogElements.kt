package dev.pellet.logging

public class PelletLogElements(
    startingElements: Map<String, PelletLogElement> = mapOf()
) {
    private val elements = startingElements.toMutableMap()

    fun add(key: String, element: PelletLogElement): PelletLogElements {
        elements += key to element
        return this
    }

    fun add(key: String, string: String?): PelletLogElements {
        elements += key to logElement(string)
        return this
    }

    fun add(key: String, number: Number?): PelletLogElements {
        elements += key to logElement(number)
        return this
    }

    fun add(key: String, boolean: Boolean?): PelletLogElements {
        elements += key to logElement(boolean)
        return this
    }

    fun add(key: String, throwable: Throwable?): PelletLogElements {
        val throwableString = throwable?.stackTraceToString()
        return add(key, throwableString)
    }

    fun add(key: String, loggable: PelletLoggable): PelletLogElements {
        elements += key to logElement(loggable)
        return this
    }

    fun all(): Map<String, PelletLogElement> {
        return elements
    }
}

public fun logElements(
    builder: PelletLogElements.() -> Unit
): () -> PelletLogElements {
    return {
        PelletLogElements(mapOf()).apply(builder)
    }
}
