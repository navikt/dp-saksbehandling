package no.nav.dagpenger.saksbehandling.db.klage

import no.nav.dagpenger.saksbehandling.klage.Opplysning
import no.nav.dagpenger.saksbehandling.klage.OpplysningType
import no.nav.dagpenger.saksbehandling.klage.Verdi
import no.nav.dagpenger.saksbehandling.serder.applyDefault
import tools.jackson.core.JsonGenerator
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.JsonNode
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.ValueSerializer
import tools.jackson.databind.module.SimpleModule
import tools.jackson.module.kotlin.jacksonMapperBuilder
import java.time.LocalDate
import java.util.UUID

object KlageOpplysningerMapper {
    private val objectMapper =
        jacksonMapperBuilder()
            .applyDefault()
            .addModule(
                SimpleModule().apply {
                    addSerializer(Verdi::class.java, VerdiSerializer())
                    addDeserializer(Verdi::class.java, VerdiDeserializer())
                },
            ).build()

    class VerdiSerializer : ValueSerializer<Verdi>() {
        companion object {
            const val VERDI_DATATYPE = "datatype"
        }

        override fun serialize(
            value: Verdi,
            gen: JsonGenerator,
            serializers: SerializationContext,
        ) {
            gen.writeStartObject()
            when (value) {
                is Verdi.TomVerdi -> gen.writeStringProperty(VERDI_DATATYPE, "TomVerdi")
                is Verdi.TekstVerdi -> {
                    gen.writeStringProperty(VERDI_DATATYPE, "TekstVerdi")
                    gen.writeStringProperty("value", value.value)
                }
                is Verdi.Dato -> {
                    gen.writeStringProperty(VERDI_DATATYPE, "Dato")
                    gen.writeStringProperty("value", value.value.toString())
                }
                is Verdi.Boolsk -> {
                    gen.writeStringProperty(VERDI_DATATYPE, "Boolsk")
                    gen.writeBooleanProperty("value", value.value)
                }
                is Verdi.Flervalg -> {
                    gen.writeStringProperty(VERDI_DATATYPE, "Flervalg")
                    gen.writeArrayPropertyStart("value")
                    value.value.forEach { gen.writeString(it) }
                    gen.writeEndArray()
                }
            }
            gen.writeEndObject()
        }
    }

    class VerdiDeserializer : ValueDeserializer<Verdi>() {
        override fun deserialize(
            p: JsonParser,
            ctxt: DeserializationContext,
        ): Verdi {
            val node: JsonNode = p.readValueAsTree()
            val type = node.get("datatype").asText()
            return when (type) {
                "TomVerdi" -> Verdi.TomVerdi
                "TekstVerdi" -> Verdi.TekstVerdi(node.get("value").asText())
                "Dato" -> Verdi.Dato(LocalDate.parse(node.get("value").asText()))
                "Boolsk" -> Verdi.Boolsk(node.get("value").asBoolean())
                "Flervalg" ->
                    Verdi.Flervalg(
                        node
                            .get("value")
                            .values()
                            .map { it.asText() },
                    )
                else -> throw IllegalArgumentException("Unknown type: $type")
            }
        }
    }

    fun Set<Opplysning>.tilJson(): String =
        objectMapper.writeValueAsString(
            this.map { opplysning ->
                mapOf(
                    "opplysningId" to opplysning.opplysningId.toString(),
                    "type" to opplysning.type.name,
                    "verdi" to opplysning.verdi(),
                    "valgmuligheter" to opplysning.valgmuligheter,
                )
            },
        )

    fun String.tilKlageOpplysninger(): Set<Opplysning> =
        objectMapper
            .readTree(this)
            .values()
            .map { jsonNode ->
                val type = OpplysningType.valueOf(jsonNode.get("type").asText())
                Opplysning(
                    opplysningId = jsonNode.get("opplysningId").asText().let { UUID.fromString(it) },
                    type = type,
                    verdi = objectMapper.convertValue(jsonNode["verdi"], Verdi::class.java),
                    valgmuligheter =
                        jsonNode
                            .get("valgmuligheter")
                            .values()
                            .map { it.asText() },
                    regler = type.regler,
                )
            }.toSet()
}
