package dev.pellet.logging

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.MDC
import java.time.Instant

class PelletStructuredLogger(
    private val name: String,
    private val levelProvider: () -> PelletLogLevel
) : PelletLogging {

    companion object {

        private const val levelKey = "level"
        private const val nameKey = "name"
        private const val messageKey = "message"
        private const val throwableKey = "throwable"
        private const val threadKey = "thread"
        private const val timestampKey = "timestamp"
        private val encoder = Json
    }

    override fun log(
        level: PelletLogLevel,
        elementsBuilder: (() -> PelletLogElements)?,
        messageBuilder: () -> String
    ) {
        if (!isLevelEnabled(level)) {
            return
        }
        val message = messageBuilder()
        val elements = elementsBuilder?.invoke()
        logStructured(level, message, elements)
    }

    override fun log(
        level: PelletLogLevel,
        throwable: Throwable?,
        elementsBuilder: (() -> PelletLogElements)?,
        messageBuilder: () -> String
    ) {
        if (!isLevelEnabled(level)) {
            return
        }
        val elements = elementsBuilder?.invoke()
        val computedElements = (elements ?: PelletLogElements())
            .add(throwableKey, throwable)
        val message = messageBuilder()
        logStructured(level, message, computedElements)
    }

    private fun logStructured(
        level: PelletLogLevel,
        message: String,
        elements: PelletLogElements?
    ) {
        val timestamp = encoder.encodeToJsonElement(InstantDateTimeSerializer, Instant.now())
        val map = mutableMapOf<String, JsonElement>(
            levelKey to JsonPrimitive(level.toString().lowercase()),
            timestampKey to timestamp.jsonPrimitive,
            messageKey to JsonPrimitive(message),
            nameKey to JsonPrimitive(name),
            threadKey to JsonPrimitive(Thread.currentThread().name)
        )
        val mdcElements = MDC.getMDCAdapter().copyOfContextMap
        if (mdcElements != null) {
            val jsonMdcElements = (mdcElements - map.keys)
                .mapValues {
                    JsonPrimitive(it.value)
                }
            map += jsonMdcElements
        }
        if (elements != null) {
            val jsonLogElements = (elements.all() - map.keys)
                .mapValues {
                    val container = it.value
                    return@mapValues when (container) {
                        is PelletLogElement.NullValue -> JsonNull
                        is PelletLogElement.StringValue -> JsonPrimitive(container.value)
                        is PelletLogElement.NumberValue -> JsonPrimitive(container.value)
                        is PelletLogElement.BooleanValue -> JsonPrimitive(container.value)
                    }
                }
            map += jsonLogElements
        }
        val json = JsonObject(map)
        val encoded = encoder.encodeToString(json)

        // Synchronize writing output to prevent multiple threads outputting at the same time
        synchronized(encoder) {
            println(encoded)
        }
    }

    private fun isLevelEnabled(level: PelletLogLevel): Boolean {
        val currentLevel = levelProvider()
        return level.value >= currentLevel.value
    }
}
