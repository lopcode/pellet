package dev.pellet.logging

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.slf4j.MDC
import org.slf4j.event.Level
import org.slf4j.helpers.MarkerIgnoringBase
import org.slf4j.helpers.MessageFormatter

public class PelletStructuredLogger(
    name: String,
    private val level: Level
) : MarkerIgnoringBase(), PelletLogging {

    companion object {

        private const val levelKey = "level"
        private const val nameKey = "name"
        private const val messageKey = "message"
        private const val throwableKey = "throwable"
        private const val threadKey = "thread"
        private val encoder = Json
    }

    init {
        this.name = name
    }

    override fun isTraceEnabled(): Boolean {
        return level >= Level.TRACE
    }

    override fun trace(msg: String?) {
        if (!isTraceEnabled) {
            return
        }
        logStructured(Level.TRACE, msg, elements = null)
    }

    override fun trace(format: String?, arg: Any?) {
        if (!isTraceEnabled) {
            return
        }
        val formattingTuple = MessageFormatter.format(format, arg)
        logStructured(Level.TRACE, formattingTuple.message, formattingTuple.throwable)
    }

    override fun trace(format: String?, arg1: Any?, arg2: Any?) {
        if (!isTraceEnabled) {
            return
        }
        val formattingTuple = MessageFormatter.format(format, arg1, arg2)
        logStructured(Level.TRACE, formattingTuple.message, formattingTuple.throwable)
    }

    override fun trace(format: String?, vararg arguments: Any?) {
        if (!isTraceEnabled) {
            return
        }
        val formattingTuple = MessageFormatter.format(format, arguments)
        logStructured(Level.TRACE, formattingTuple.message, formattingTuple.throwable)
    }

    override fun trace(msg: String?, t: Throwable?) {
        if (!isTraceEnabled) {
            return
        }
        logStructured(Level.TRACE, msg, t)
    }

    override fun isDebugEnabled(): Boolean {
        return level >= Level.DEBUG
    }

    override fun debug(msg: String?) {
        if (!isDebugEnabled) {
            return
        }
        logStructured(Level.DEBUG, msg, elements = null)
    }

    override fun debug(format: String?, arg: Any?) {
        if (!isDebugEnabled) {
            return
        }
        val formattingTuple = MessageFormatter.format(format, arg)
        logStructured(Level.DEBUG, formattingTuple.message, formattingTuple.throwable)
    }

    override fun debug(format: String?, arg1: Any?, arg2: Any?) {
        if (!isDebugEnabled) {
            return
        }
        val formattingTuple = MessageFormatter.format(format, arg1, arg2)
        logStructured(Level.DEBUG, formattingTuple.message, formattingTuple.throwable)
    }

    override fun debug(format: String?, vararg arguments: Any?) {
        if (!isDebugEnabled) {
            return
        }
        val formattingTuple = MessageFormatter.format(format, arguments)
        logStructured(Level.DEBUG, formattingTuple.message, formattingTuple.throwable)
    }

    override fun debug(msg: String?, t: Throwable?) {
        if (!isDebugEnabled) {
            return
        }
        logStructured(Level.DEBUG, msg, t)
    }

    override fun isInfoEnabled(): Boolean {
        return level >= Level.INFO
    }

    override fun info(msg: String?) {
        if (!isInfoEnabled) {
            return
        }
        logStructured(Level.INFO, msg, elements = null)
    }

    override fun info(format: String?, arg: Any?) {
        if (!isInfoEnabled) {
            return
        }
        val formattingTuple = MessageFormatter.format(format, arg)
        logStructured(Level.INFO, formattingTuple.message, formattingTuple.throwable)
    }

    override fun info(format: String?, arg1: Any?, arg2: Any?) {
        if (!isInfoEnabled) {
            return
        }
        val formattingTuple = MessageFormatter.format(format, arg1, arg2)
        logStructured(Level.INFO, formattingTuple.message, formattingTuple.throwable)
    }

    override fun info(format: String?, vararg arguments: Any?) {
        if (!isInfoEnabled) {
            return
        }
        val formattingTuple = MessageFormatter.format(format, arguments)
        logStructured(Level.INFO, formattingTuple.message, formattingTuple.throwable)
    }

    override fun info(msg: String?, t: Throwable?) {
        if (!isInfoEnabled) {
            return
        }
        logStructured(Level.INFO, msg, t)
    }

    override fun isWarnEnabled(): Boolean {
        return level >= Level.WARN
    }

    override fun warn(msg: String?) {
        if (!isWarnEnabled) {
            return
        }
        logStructured(Level.WARN, msg, elements = null)
    }

    override fun warn(format: String?, arg: Any?) {
        if (!isWarnEnabled) {
            return
        }
        val formattingTuple = MessageFormatter.format(format, arg)
        logStructured(Level.WARN, formattingTuple.message, formattingTuple.throwable)
    }

    override fun warn(format: String?, vararg arguments: Any?) {
        if (!isWarnEnabled) {
            return
        }
        val formattingTuple = MessageFormatter.format(format, arguments)
        logStructured(Level.WARN, formattingTuple.message, formattingTuple.throwable)
    }

    override fun warn(format: String?, arg1: Any?, arg2: Any?) {
        if (!isWarnEnabled) {
            return
        }
        val formattingTuple = MessageFormatter.format(format, arg1, arg2)
        logStructured(Level.WARN, formattingTuple.message, formattingTuple.throwable)
    }

    override fun warn(msg: String?, t: Throwable?) {
        if (!isWarnEnabled) {
            return
        }
        logStructured(Level.WARN, msg, t)
    }

    override fun isErrorEnabled(): Boolean {
        @Suppress("KotlinConstantConditions")
        return level >= Level.ERROR
    }

    override fun error(msg: String?) {
        if (!isErrorEnabled) {
            return
        }
        logStructured(Level.ERROR, msg, elements = null)
    }

    override fun error(format: String?, arg: Any?) {
        if (!isErrorEnabled) {
            return
        }
        val formattingTuple = MessageFormatter.format(format, arg)
        logStructured(Level.ERROR, formattingTuple.message, formattingTuple.throwable)
    }

    override fun error(format: String?, arg1: Any?, arg2: Any?) {
        if (!isErrorEnabled) {
            return
        }
        val formattingTuple = MessageFormatter.format(format, arg1, arg2)
        logStructured(Level.ERROR, formattingTuple.message, formattingTuple.throwable)
    }

    override fun error(format: String?, vararg arguments: Any?) {
        if (!isErrorEnabled) {
            return
        }
        val formattingTuple = MessageFormatter.format(format, arguments)
        logStructured(Level.ERROR, formattingTuple.message, formattingTuple.throwable)
    }

    override fun error(msg: String?, t: Throwable?) {
        if (!isErrorEnabled) {
            return
        }
        logStructured(Level.ERROR, msg, t)
    }

    private fun logStructured(
        level: Level,
        message: String?,
        throwable: Throwable?
    ) {
        if (throwable == null) {
            return logStructured(level, message, elements = null)
        }
        val stacktrace = throwable.stackTraceToString()
        val elements = PelletLoggingElements(
            mapOf(throwableKey to JsonPrimitive(stacktrace))
        )
        logStructured(Level.TRACE, message, elements)
    }

    private fun logStructured(
        level: Level,
        message: String?,
        elements: PelletLoggingElements?
    ) {
        val map = mutableMapOf(
            levelKey to JsonPrimitive(level.toString().lowercase()),
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
            map += (elements.all().toMap() - map.keys)
        }
        val json = JsonObject(map)
        val encoded = encoder.encodeToString(json)

        // Synchronize writing output to prevent multiple threads outputting at the same time
        synchronized(encoder) {
            println(encoded)
        }
    }
}
