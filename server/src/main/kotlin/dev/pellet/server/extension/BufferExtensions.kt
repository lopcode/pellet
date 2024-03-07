package dev.pellet.server.extension

import kotlinx.io.Buffer
import kotlinx.io.Source
import kotlinx.io.indexOf
import kotlinx.io.readString
import java.nio.charset.Charset

internal fun Buffer.stringifyAndClear(charset: Charset = Charsets.US_ASCII): String {
    val string = this.readString(charset)
    this.clear()
    return string
}

internal fun Source.indexOfOrNull(needle: Byte): Long? {
    val position = this.indexOf(needle)
    if (position < 0) {
        return null
    }
    return position
}
