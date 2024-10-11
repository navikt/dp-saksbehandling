package no.nav.dagpenger.saksbehandling.hendelser

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.dagpenger.saksbehandling.Applikasjon
import java.time.LocalDateTime
import java.util.UUID

data class SøknadsbehandlingOpprettetHendelse(
    val søknadId: UUID,
    val behandlingId: UUID,
    val ident: String,
    val opprettet: LocalDateTime,
    override val utførtAv: Applikasjon = Applikasjon("dp-behandling"),
) : Hendelse(utførtAv) {
    companion object {
        private val objectMapper: ObjectMapper =
            jacksonObjectMapper()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .enable(SerializationFeature.INDENT_OUTPUT)

        fun fromJson(json: String): SøknadsbehandlingOpprettetHendelse {
            val jsonTree = objectMapper.readTree(json)

            return SøknadsbehandlingOpprettetHendelse(
                søknadId = jsonTree["søknadId"].asText().let(UUID::fromString),
                behandlingId = jsonTree["behandlingId"].asText().let(UUID::fromString),
                ident = jsonTree["ident"].asText(),
                opprettet = LocalDateTime.parse(jsonTree["opprettet"].asText()),
            )
        }
    }
}
