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
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.TilgangType

internal val objectMapper: ObjectMapper =
    jacksonObjectMapper().apply {
        val behandlerModule = SimpleModule().addDeserializer(Behandler::class.java, BehandlerDeserializer())

        registerModule(JavaTimeModule())
        registerModule(behandlerModule)
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        setSerializationInclusion(JsonInclude.Include.NON_NULL)
        enable(SerializationFeature.INDENT_OUTPUT)
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
            Applikasjon(
                navn = node["navn"].asText(),
            )
        }
    }
}
