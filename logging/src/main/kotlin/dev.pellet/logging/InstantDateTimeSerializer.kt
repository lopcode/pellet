package dev.pellet.logging

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant
import java.time.format.DateTimeFormatter

object InstantDateTimeSerializer : KSerializer<Instant> {

    override val descriptor = PrimitiveSerialDescriptor(
        "Instant", PrimitiveKind.STRING
    )

    override fun serialize(
        encoder: Encoder,
        value: Instant
    ) {
        val format = DateTimeFormatter.ISO_INSTANT
        val string = format.format(value)
        encoder.encodeString(string)
    }

    override fun deserialize(
        decoder: Decoder
    ): Instant {
        val string = decoder.decodeString()
        return Instant.parse(string)
    }
}
