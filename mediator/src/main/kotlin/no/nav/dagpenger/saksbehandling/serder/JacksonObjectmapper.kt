package no.nav.dagpenger.saksbehandling.serder

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.dagpenger.saksbehandling.hendelser.Aktør

internal val objectMapper: ObjectMapper =
    jacksonObjectMapper()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .enable(SerializationFeature.INDENT_OUTPUT)
        .registerModule(JavaTimeModule())
        .registerModule(AktørSerDerModule())

private class AktørSerDerModule : SimpleModule() {
    init {
        addSerializer(Aktør::class.java, AktørSerializer)
        addDeserializer(Aktør::class.java, AktørDeserializer)
    }

    private object AktørSerializer : JsonSerializer<Aktør>() {
        override fun serialize(
            value: Aktør,
            gen: JsonGenerator,
            serializers: SerializerProvider,
        ) {
            gen.writeStartObject()
            when (value) {
                is Aktør.Ukjent -> {
                    gen.writeStringField("type", "Ukjent")
                }

                is Aktør.Saksbehandler -> {
                    gen.writeStringField("type", "Saksbehandler")
                    gen.writeStringField("navIdent", value.navIdent)
                }

                is Aktør.System -> {
                    gen.writeStringField("type", "System")
                    gen.writeStringField("navn", value.navn)
                }
            }
            gen.writeEndObject()
        }
    }

    private object AktørDeserializer : JsonDeserializer<Aktør>() {
        override fun deserialize(
            p: JsonParser,
            ctxt: DeserializationContext,
        ): Aktør {
            val node: JsonNode = p.codec.readTree(p)
            val type = node.get("type").asText()
            return when (type) {
                "Ukjent" -> Aktør.Ukjent
                "Saksbehandler" -> Aktør.Saksbehandler(node.get("navIdent").asText())
                "System" -> Aktør.System(node.get("navn").asText())
                else -> throw IllegalArgumentException("Unknown type: $type")
            }
        }
    }
}
