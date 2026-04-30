package no.nav.dagpenger.saksbehandling.serder

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.dagpenger.saksbehandling.Applikasjon
import no.nav.dagpenger.saksbehandling.Behandler
import no.nav.dagpenger.saksbehandling.HendelseBehandler
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.TilgangType

fun defaultObjectMapper(): ObjectMapper = jacksonObjectMapper().applyDefault()

fun ObjectMapper.applyDefault(): ObjectMapper =
    this.apply {
        registerModule(JavaTimeModule())
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    }

internal val objectMapper: ObjectMapper =
    defaultObjectMapper().apply {
        val behandlerModule = SimpleModule().addDeserializer(Behandler::class.java, BehandlerDeserializer())
        val applikasjonModule = SimpleModule().addDeserializer(Applikasjon::class.java, ApplikasjonDeserializer())
        val utløstAvTypeModule =
            SimpleModule()
                .addSerializer(HendelseBehandler::class.java, HendelseBehandlerSerializer())
                .addDeserializer(HendelseBehandler::class.java, HendelseBehandlerDeserializer())

        registerModule(applikasjonModule)
        registerModule(behandlerModule)
        registerModule(utløstAvTypeModule)
        setSerializationInclusion(JsonInclude.Include.NON_NULL)
        enable(SerializationFeature.INDENT_OUTPUT)
    }

internal class ApplikasjonDeserializer : JsonDeserializer<Applikasjon>() {
    override fun deserialize(
        parser: JsonParser,
        ctxt: DeserializationContext,
    ): Applikasjon {
        val node: JsonNode = parser.codec.readTree(parser)

        return Applikasjon.fra(node["navn"].asText())
    }
}

internal class BehandlerDeserializer : JsonDeserializer<Behandler>() {
    override fun deserialize(
        parser: JsonParser,
        ctxt: DeserializationContext,
    ): Behandler {
        val node: JsonNode = parser.codec.readTree(parser)

        return if (node.has("navIdent")) {
            Saksbehandler(
                navIdent = node["navIdent"].asText(),
                grupper = node["grupper"].map { it.asText() }.toSet(),
                tilganger = node["tilganger"].map { TilgangType.valueOf(it.asText()) }.toSet(),
            )
        } else {
            Applikasjon.fra(node["navn"].asText())
        }
    }
}

internal class HendelseBehandlerSerializer : com.fasterxml.jackson.databind.JsonSerializer<HendelseBehandler>() {
    override fun serialize(
        value: HendelseBehandler,
        gen: com.fasterxml.jackson.core.JsonGenerator,
        serializers: com.fasterxml.jackson.databind.SerializerProvider,
    ) {
        gen.writeString(value.name)
    }
}

internal class HendelseBehandlerDeserializer : JsonDeserializer<HendelseBehandler>() {
    override fun deserialize(
        parser: JsonParser,
        ctxt: DeserializationContext,
    ): HendelseBehandler = HendelseBehandler.valueOf(parser.text)
}
