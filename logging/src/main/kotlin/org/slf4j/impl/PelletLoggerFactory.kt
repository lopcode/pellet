package org.slf4j.impl

import dev.pellet.logging.PelletLogging
import dev.pellet.logging.PelletSLF4JBridge
import org.slf4j.ILoggerFactory
import org.slf4j.Logger

class PelletLoggerFactory : ILoggerFactory {

    override fun getLogger(name: String): Logger {
        return PelletSLF4JBridge(name, PelletLogging.level)
    }
}
