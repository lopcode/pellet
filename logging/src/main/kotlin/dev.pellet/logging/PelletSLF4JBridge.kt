package dev.pellet.logging

import org.slf4j.helpers.FormattingTuple
import org.slf4j.helpers.MarkerIgnoringBase
import org.slf4j.helpers.MessageFormatter

public class PelletSLF4JBridge(
    name: String,
    private val level: PelletLogLevel
) : MarkerIgnoringBase() {

    private val backingLogger = pelletLogger(name)

    override fun isTraceEnabled(): Boolean {
        return level.value >= PelletLogLevel.TRACE.value
    }

    override fun trace(msg: String?) {
        if (!isTraceEnabled) {
            return
        }
        backingLogger.log(PelletLogLevel.TRACE) { msg ?: "" }
    }

    override fun trace(format: String?, arg: Any?) {
        if (!isTraceEnabled) {
            return
        }
        val formattingTuple = MessageFormatter.format(format, arg)
        logFormattingTuple(PelletLogLevel.TRACE, formattingTuple)
    }

    override fun trace(format: String?, arg1: Any?, arg2: Any?) {
        if (!isTraceEnabled) {
            return
        }
        val formattingTuple = MessageFormatter.format(format, arg1, arg2)
        logFormattingTuple(PelletLogLevel.TRACE, formattingTuple)
    }

    override fun trace(format: String?, vararg arguments: Any?) {
        if (!isTraceEnabled) {
            return
        }
        val formattingTuple = MessageFormatter.format(format, arguments)
        logFormattingTuple(PelletLogLevel.TRACE, formattingTuple)
    }

    override fun trace(msg: String?, t: Throwable?) {
        if (!isTraceEnabled) {
            return
        }
        log(PelletLogLevel.TRACE, msg, t)
    }

    override fun isDebugEnabled(): Boolean {
        return level.value >= PelletLogLevel.DEBUG.value
    }

    override fun debug(msg: String?) {
        if (!isDebugEnabled) {
            return
        }
        log(PelletLogLevel.DEBUG, msg, null)
    }

    override fun debug(format: String?, arg: Any?) {
        if (!isDebugEnabled) {
            return
        }
        val formattingTuple = MessageFormatter.format(format, arg)
        logFormattingTuple(PelletLogLevel.DEBUG, formattingTuple)
    }

    override fun debug(format: String?, arg1: Any?, arg2: Any?) {
        if (!isDebugEnabled) {
            return
        }
        val formattingTuple = MessageFormatter.format(format, arg1, arg2)
        logFormattingTuple(PelletLogLevel.DEBUG, formattingTuple)
    }

    override fun debug(format: String?, vararg arguments: Any?) {
        if (!isDebugEnabled) {
            return
        }
        val formattingTuple = MessageFormatter.format(format, arguments)
        logFormattingTuple(PelletLogLevel.DEBUG, formattingTuple)
    }

    override fun debug(msg: String?, t: Throwable?) {
        if (!isDebugEnabled) {
            return
        }
        log(PelletLogLevel.DEBUG, msg, t)
    }

    override fun isInfoEnabled(): Boolean {
        return level.value >= PelletLogLevel.INFO.value
    }

    override fun info(msg: String?) {
        if (!isInfoEnabled) {
            return
        }
        log(PelletLogLevel.INFO, msg, null)
    }

    override fun info(format: String?, arg: Any?) {
        if (!isInfoEnabled) {
            return
        }
        val formattingTuple = MessageFormatter.format(format, arg)
        logFormattingTuple(PelletLogLevel.INFO, formattingTuple)
    }

    override fun info(format: String?, arg1: Any?, arg2: Any?) {
        if (!isInfoEnabled) {
            return
        }
        val formattingTuple = MessageFormatter.format(format, arg1, arg2)
        logFormattingTuple(PelletLogLevel.INFO, formattingTuple)
    }

    override fun info(format: String?, vararg arguments: Any?) {
        if (!isInfoEnabled) {
            return
        }
        val formattingTuple = MessageFormatter.format(format, arguments)
        logFormattingTuple(PelletLogLevel.INFO, formattingTuple)
    }

    override fun info(msg: String?, t: Throwable?) {
        if (!isInfoEnabled) {
            return
        }
        log(PelletLogLevel.INFO, msg, t)
    }

    override fun isWarnEnabled(): Boolean {
        return level.value >= PelletLogLevel.WARN.value
    }

    override fun warn(msg: String?) {
        if (!isWarnEnabled) {
            return
        }
        log(PelletLogLevel.WARN, msg, null)
    }

    override fun warn(format: String?, arg: Any?) {
        if (!isWarnEnabled) {
            return
        }
        val formattingTuple = MessageFormatter.format(format, arg)
        logFormattingTuple(PelletLogLevel.WARN, formattingTuple)
    }

    override fun warn(format: String?, vararg arguments: Any?) {
        if (!isWarnEnabled) {
            return
        }
        val formattingTuple = MessageFormatter.format(format, arguments)
        logFormattingTuple(PelletLogLevel.WARN, formattingTuple)
    }

    override fun warn(format: String?, arg1: Any?, arg2: Any?) {
        if (!isWarnEnabled) {
            return
        }
        val formattingTuple = MessageFormatter.format(format, arg1, arg2)
        logFormattingTuple(PelletLogLevel.WARN, formattingTuple)
    }

    override fun warn(msg: String?, t: Throwable?) {
        if (!isWarnEnabled) {
            return
        }
        log(PelletLogLevel.WARN, msg, t)
    }

    override fun isErrorEnabled(): Boolean {
        return level.value >= PelletLogLevel.ERROR.value
    }

    override fun error(msg: String?) {
        if (!isErrorEnabled) {
            return
        }
        log(PelletLogLevel.ERROR, msg, null)
    }

    override fun error(format: String?, arg: Any?) {
        if (!isErrorEnabled) {
            return
        }
        val formattingTuple = MessageFormatter.format(format, arg)
        logFormattingTuple(PelletLogLevel.ERROR, formattingTuple)
    }

    override fun error(format: String?, arg1: Any?, arg2: Any?) {
        if (!isErrorEnabled) {
            return
        }
        val formattingTuple = MessageFormatter.format(format, arg1, arg2)
        logFormattingTuple(PelletLogLevel.ERROR, formattingTuple)
    }

    override fun error(format: String?, vararg arguments: Any?) {
        if (!isErrorEnabled) {
            return
        }
        val formattingTuple = MessageFormatter.format(format, arguments)
        logFormattingTuple(PelletLogLevel.ERROR, formattingTuple)
    }

    override fun error(msg: String?, t: Throwable?) {
        if (!isErrorEnabled) {
            return
        }
        log(PelletLogLevel.ERROR, msg, t)
    }

    private fun logFormattingTuple(
        level: PelletLogLevel,
        formattingTuple: FormattingTuple
    ) {
        log(level, formattingTuple.message, formattingTuple.throwable)
    }

    private fun log(
        level: PelletLogLevel,
        message: String?,
        throwable: Throwable?
    ) {
        if (throwable != null) {
            backingLogger.log(level, throwable) { message ?: "" }
        } else {
            backingLogger.log(level) { message ?: "" }
        }
    }
}
