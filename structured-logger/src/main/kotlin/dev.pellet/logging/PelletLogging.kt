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
        elements: PelletLogElements? = null,
        messageBuilder: () -> String
    )
    fun log(
        level: PelletLogLevel,
        throwable: Throwable?,
        elements: PelletLogElements? = null,
        messageBuilder: () -> String
    )

    fun error(elements: PelletLogElements? = null, messageBuilder: () -> String)
    fun error(throwable: Throwable?, elements: PelletLogElements? = null, messageBuilder: () -> String)

    fun warn(elements: PelletLogElements? = null, messageBuilder: () -> String)
    fun warn(throwable: Throwable?, elements: PelletLogElements? = null, messageBuilder: () -> String)

    fun info(elements: PelletLogElements? = null, messageBuilder: () -> String)
    fun info(throwable: Throwable?, elements: PelletLogElements? = null, messageBuilder: () -> String)

    fun debug(elements: PelletLogElements? = null, messageBuilder: () -> String)
    fun debug(throwable: Throwable?, elements: PelletLogElements? = null, messageBuilder: () -> String)

    fun trace(elements: PelletLogElements? = null, messageBuilder: () -> String)
    fun trace(throwable: Throwable?, elements: PelletLogElements? = null, messageBuilder: () -> String)
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
