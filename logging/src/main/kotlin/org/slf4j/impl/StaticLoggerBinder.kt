package org.slf4j.impl

import org.slf4j.ILoggerFactory

class StaticLoggerBinder {

    companion object {
        private val singleton = StaticLoggerBinder()

        @JvmStatic
        fun getSingleton(): StaticLoggerBinder {
            return singleton
        }
    }

    fun getLoggerFactory(): ILoggerFactory {
        return PelletLoggerFactory()
    }
}
