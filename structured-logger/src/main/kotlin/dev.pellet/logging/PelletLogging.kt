package dev.pellet.logging

import org.slf4j.Logger
import org.slf4j.event.Level

val defaultLevel = Level.INFO

public interface PelletLogging : Logger

public fun pelletLogger(
    name: String,
    level: Level = defaultLevel
): PelletLogging {
    return PelletStructuredLogger(name, level)
}

public fun <T> pelletLogger(
    clazz: Class<T>,
    level: Level = defaultLevel
): PelletLogging {
    return pelletLogger(clazz.name, level)
}

public inline fun <reified T> pelletLogger(
    level: Level = defaultLevel
): PelletLogging {
    return pelletLogger(T::class.java, level)
}
