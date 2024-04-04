package dev.pellet.logging.slf4j

import dev.pellet.logging.PelletLogging
import org.slf4j.ILoggerFactory
import org.slf4j.Logger

class PelletSLF4JLoggerFactory : ILoggerFactory {

    override fun getLogger(name: String): Logger {
        return PelletSLF4JBridge(name, PelletLogging.level)
    }
}
