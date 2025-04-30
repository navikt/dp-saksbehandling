package no.nav.dagpenger.saksbehandling.db.klage

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import no.nav.dagpenger.saksbehandling.klage.Opplysning
import no.nav.dagpenger.saksbehandling.klage.OpplysningType
import no.nav.dagpenger.saksbehandling.klage.Verdi
import java.time.LocalDate
import java.util.UUID

object KlageOpplysningerMapper {
    private val objectMapper =
        ObjectMapper().also {
            it.registerModule(JavaTimeModule())
            it.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            it.registerModules(
                SimpleModule().apply {
                    addSerializer(Verdi::class.java, VerdiSerializer())
                    addDeserializer(Verdi::class.java, VerdiDeserializer())
                },
            )
        }

    // Custom serializer and deserializer for Verdi
    class VerdiSerializer : JsonSerializer<Verdi>() {
        companion object {
            const val VERDI_DATATYPE = "datatype"
        }

        override fun serialize(
            value: Verdi,
            gen: JsonGenerator,
            serializers: SerializerProvider,
        ) {
            gen.writeStartObject()
            when (value) {
                is Verdi.TomVerdi -> gen.writeStringField(VERDI_DATATYPE, "TomVerdi")
                is Verdi.TekstVerdi -> {
                    gen.writeStringField(VERDI_DATATYPE, "TekstVerdi")
                    gen.writeStringField("value", value.value)
                }
                is Verdi.Dato -> {
                    gen.writeStringField(VERDI_DATATYPE, "Dato")
                    gen.writeStringField("value", value.value.toString())
                }
                is Verdi.Boolsk -> {
                    gen.writeStringField(VERDI_DATATYPE, "Boolsk")
                    gen.writeBooleanField("value", value.value)
                }
                is Verdi.Flervalg -> {
                    gen.writeStringField(VERDI_DATATYPE, "Flervalg")
                    gen.writeArrayFieldStart("value")
                    value.value.forEach { gen.writeString(it) }
                    gen.writeEndArray()
                }
            }
            gen.writeEndObject()
        }
    }

    class VerdiDeserializer : JsonDeserializer<Verdi>() {
        override fun deserialize(
            p: JsonParser,
            ctxt: DeserializationContext,
        ): Verdi {
            val node = p.codec.readTree<JsonNode>(p)
            val type = node.get("datatype").asText()
            return when (type) {
                "TomVerdi" -> Verdi.TomVerdi
                "TekstVerdi" -> Verdi.TekstVerdi(node.get("value").asText())
                "Dato" -> Verdi.Dato(LocalDate.parse(node.get("value").asText()))
                "Boolsk" -> Verdi.Boolsk(node.get("value").asBoolean())
                "Flervalg" -> Verdi.Flervalg(node.get("value").map { it.asText() })
                else -> throw IllegalArgumentException("Unknown type: $type")
            }
        }
    }

    fun Set<Opplysning>.tilJson(): String {
        return objectMapper.writeValueAsString(this)
    }

    fun String.tilKlageOpplysninger(): Set<Opplysning> {
        return objectMapper.readTree(this).map { jsonNode ->
            Opplysning(
                opplysningId = jsonNode.get("opplysningId").asText().let { UUID.fromString(it) },
                type = OpplysningType.valueOf(jsonNode.get("type").asText()),
                verdi = objectMapper.convertValue(jsonNode["verdi"], Verdi::class.java),
            )
        }.toSet()
    }
}
