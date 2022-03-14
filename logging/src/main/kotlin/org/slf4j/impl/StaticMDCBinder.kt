package org.slf4j.impl

import org.slf4j.helpers.BasicMDCAdapter
import org.slf4j.spi.MDCAdapter

class StaticMDCBinder {

    companion object {
        private val singleton = StaticMDCBinder()

        @JvmStatic
        fun getSingleton(): StaticMDCBinder {
            return singleton
        }
    }

    fun getMDCA(): MDCAdapter {
        return BasicMDCAdapter()
    }

    fun getMDCAdapterClassStr(): String {
        return BasicMDCAdapter::class.java.name
    }
}
