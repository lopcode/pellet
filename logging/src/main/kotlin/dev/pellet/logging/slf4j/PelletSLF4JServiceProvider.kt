package dev.pellet.logging.slf4j

import org.slf4j.ILoggerFactory
import org.slf4j.IMarkerFactory
import org.slf4j.helpers.BasicMDCAdapter
import org.slf4j.helpers.BasicMarkerFactory
import org.slf4j.spi.MDCAdapter
import org.slf4j.spi.SLF4JServiceProvider

class PelletSLF4JServiceProvider : SLF4JServiceProvider {

    private lateinit var storedLoggerFactory: ILoggerFactory
    private lateinit var storedMarkerFactory: IMarkerFactory
    private lateinit var storedMDCAdapter: MDCAdapter

    override fun getLoggerFactory(): ILoggerFactory {
        return storedLoggerFactory
    }

    override fun getMarkerFactory(): IMarkerFactory {
        return storedMarkerFactory
    }

    override fun getMDCAdapter(): MDCAdapter {
        return storedMDCAdapter
    }

    override fun getRequestedApiVersion(): String {
        return "2.0.99"
    }

    override fun initialize() {
        storedLoggerFactory = PelletSLF4JLoggerFactory()
        storedMarkerFactory = BasicMarkerFactory()
        storedMDCAdapter = BasicMDCAdapter()
    }
}
