package no.nav.dagpenger.saksbehandling.serder

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.dagpenger.saksbehandling.Applikasjon
import no.nav.dagpenger.saksbehandling.Behandler
import no.nav.dagpenger.saksbehandling.HendelseBehandler
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.TilgangType
import tools.jackson.core.JsonGenerator
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.ValueSerializer
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.introspect.DefaultAccessorNamingStrategy
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.module.SimpleModule
import tools.jackson.module.kotlin.jacksonMapperBuilder

/**
 * Konfigurerer en JsonMapper.Builder med standardinnstillinger for Dagpenger-applikasjoner.
 * Brukes av Ktor jackson3-plugin (som gir en Builder i lambdaen).
 */
fun JsonMapper.Builder.applyDefault(): JsonMapper.Builder =
    this
        .accessorNaming(
            DefaultAccessorNamingStrategy
                .Provider()
                .withFirstCharAcceptance(true, true),
        ).disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)

fun defaultObjectMapper(): ObjectMapper =
    jacksonMapperBuilder()
        .applyDefault()
        .build()

internal val objectMapper: ObjectMapper =
    jacksonMapperBuilder()
        .applyDefault()
        .addModule(SimpleModule().addDeserializer(Behandler::class.java, BehandlerDeserializer()))
        .addModule(SimpleModule().addDeserializer(Applikasjon::class.java, ApplikasjonDeserializer()))
        .addModule(
            SimpleModule()
                .addSerializer(HendelseBehandler::class.java, HendelseBehandlerSerializer())
                .addDeserializer(HendelseBehandler::class.java, HendelseBehandlerDeserializer()),
        ).changeDefaultPropertyInclusion { JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.USE_DEFAULTS) }
        .enable(SerializationFeature.INDENT_OUTPUT)
        .build()

internal class ApplikasjonDeserializer : ValueDeserializer<Applikasjon>() {
    override fun deserialize(
        parser: JsonParser,
        ctxt: DeserializationContext,
    ): Applikasjon {
        val node: JsonNode = parser.readValueAsTree()

        return Applikasjon.fra(node["navn"].asText())
    }
}

internal class BehandlerDeserializer : ValueDeserializer<Behandler>() {
    override fun deserialize(
        parser: JsonParser,
        ctxt: DeserializationContext,
    ): Behandler {
        val node: JsonNode = parser.readValueAsTree()

        return if (node.has("navIdent")) {
            Saksbehandler(
                navIdent = node["navIdent"].asText(),
                grupper =
                    node["grupper"]
                        .values()
                        .map { it.asText() }
                        .toSet(),
                tilganger =
                    node["tilganger"]
                        .values()
                        .map { TilgangType.valueOf(it.asText()) }
                        .toSet(),
            )
        } else {
            Applikasjon.fra(node["navn"].asText())
        }
    }
}

internal class HendelseBehandlerSerializer : ValueSerializer<HendelseBehandler>() {
    override fun serialize(
        value: HendelseBehandler,
        gen: JsonGenerator,
        serializers: SerializationContext,
    ) {
        gen.writeString(value.name)
    }
}

internal class HendelseBehandlerDeserializer : ValueDeserializer<HendelseBehandler>() {
    override fun deserialize(
        parser: JsonParser,
        ctxt: DeserializationContext,
    ): HendelseBehandler = HendelseBehandler.valueOf(parser.text)
}
