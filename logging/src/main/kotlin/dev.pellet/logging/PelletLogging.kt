package dev.pellet.logging

import java.util.concurrent.atomic.AtomicReference

public interface PelletLogging {

    companion object {

        private val internalLevel = AtomicReference(PelletLogLevel.INFO)
        var level: PelletLogLevel
            get() = internalLevel.get()
            set(newValue) = internalLevel.set(newValue)
    }

    fun log(
        level: PelletLogLevel,
        elementsBuilder: (() -> PelletLogElements)? = null,
        messageBuilder: () -> String
    )

    fun log(
        level: PelletLogLevel,
        throwable: Throwable? = null,
        elementsBuilder: (() -> PelletLogElements)? = null,
        messageBuilder: () -> String
    )

    fun error(
        elementsBuilder: (() -> PelletLogElements)? = null,
        messageBuilder: () -> String
    ) {
        log(PelletLogLevel.ERROR, elementsBuilder, messageBuilder)
    }

    fun error(
        throwable: Throwable? = null,
        elementsBuilder: (() -> PelletLogElements)? = null,
        messageBuilder: () -> String
    ) {
        log(PelletLogLevel.ERROR, throwable, elementsBuilder, messageBuilder)
    }

    fun warn(
        elementsBuilder: (() -> PelletLogElements)? = null,
        messageBuilder: () -> String
    ) {
        log(PelletLogLevel.WARN, elementsBuilder, messageBuilder)
    }

    fun warn(
        throwable: Throwable? = null,
        elementsBuilder: (() -> PelletLogElements)? = null,
        messageBuilder: () -> String
    ) {
        log(PelletLogLevel.WARN, throwable, elementsBuilder, messageBuilder)
    }

    fun info(
        elementsBuilder: (() -> PelletLogElements)? = null,
        messageBuilder: () -> String
    ) {
        log(PelletLogLevel.INFO, elementsBuilder, messageBuilder)
    }

    fun info(
        throwable: Throwable? = null,
        elementsBuilder: (() -> PelletLogElements)? = null,
        messageBuilder: () -> String
    ) {
        log(PelletLogLevel.INFO, throwable, elementsBuilder, messageBuilder)
    }

    fun debug(
        elementsBuilder: (() -> PelletLogElements)? = null,
        messageBuilder: () -> String
    ) {
        log(PelletLogLevel.DEBUG, elementsBuilder, messageBuilder)
    }

    fun debug(
        throwable: Throwable? = null,
        elementsBuilder: (() -> PelletLogElements)? = null,
        messageBuilder: () -> String
    ) {
        log(PelletLogLevel.DEBUG, throwable, elementsBuilder, messageBuilder)
    }

    fun trace(
        elementsBuilder: (() -> PelletLogElements)? = null,
        messageBuilder: () -> String
    ) {
        log(PelletLogLevel.TRACE, elementsBuilder, messageBuilder)
    }

    fun trace(
        throwable: Throwable? = null,
        elementsBuilder: (() -> PelletLogElements)? = null,
        messageBuilder: () -> String
    ) {
        log(PelletLogLevel.TRACE, throwable, elementsBuilder, messageBuilder)
    }
}

public fun pelletLogger(
    name: String
): PelletLogging {
    return PelletStructuredLogger(name) { PelletLogging.level }
}

public fun <T> pelletLogger(
    clazz: Class<T>
): PelletLogging {
    return pelletLogger(clazz.name)
}

public inline fun <reified T> pelletLogger(): PelletLogging {
    return pelletLogger(T::class.java)
}
