package dev.pellet.logging

// Boxes primitive types to make sure that structured log elements can be serialised properly at the point of printing
public sealed class PelletLogElement {

    object NullValue : PelletLogElement()
    data class StringValue(val value: String) : PelletLogElement()
    data class NumberValue(val value: Number) : PelletLogElement()
    data class BooleanValue(val value: Boolean) : PelletLogElement()
}

public interface PelletLoggable {

    fun toLogElement(): PelletLogElement
}

public fun logElement(value: String?): PelletLogElement {
    if (value == null) {
        return PelletLogElement.NullValue
    }
    return PelletLogElement.StringValue(value)
}

public fun logElement(value: Number?): PelletLogElement {
    if (value == null) {
        return PelletLogElement.NullValue
    }
    return PelletLogElement.NumberValue(value)
}

public fun logElement(value: Boolean?): PelletLogElement {
    if (value == null) {
        return PelletLogElement.NullValue
    }
    return PelletLogElement.BooleanValue(value)
}

public fun logElement(loggable: PelletLoggable): PelletLogElement {
    return loggable.toLogElement()
}
