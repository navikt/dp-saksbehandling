package no.nav.dagpenger.saksbehandling.hendelser

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.dagpenger.saksbehandling.Applikasjon
import no.nav.dagpenger.saksbehandling.Behandler
import no.nav.dagpenger.saksbehandling.BehandlingType
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.TilgangType
import java.time.LocalDateTime
import java.util.UUID

data class BehandlingOpprettetHendelse(
    val behandlingId: UUID,
    val ident: String,
    val opprettet: LocalDateTime,
    val type: BehandlingType,
    override val utførtAv: Behandler = Applikasjon("dp-mottak"),
) : Hendelse(utførtAv) {
    companion object {
        private val objectMapper: ObjectMapper =
            jacksonObjectMapper()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .enable(SerializationFeature.INDENT_OUTPUT)

        fun fromJson(json: String): BehandlingOpprettetHendelse {
            val jsonTree = objectMapper.readTree(json)

            val erSaksbehandler = jsonTree.get("utførtAv")?.get("navIdent")?.isNull == false

            val utførtAv =
                if (erSaksbehandler) {
                    Saksbehandler(
                        navIdent = jsonTree["utførtAv"]["navIdent"].asText(),
                        grupper = jsonTree["utførtAv"]["grupper"].map { it.asText() }.toSet(),
                        tilganger = jsonTree["utførtAv"]["tilganger"].map { TilgangType.valueOf(it.asText()) }.toSet(),
                    )
                } else {
                    Applikasjon(jsonTree["utførtAv"]["navn"].asText())
                }

            return BehandlingOpprettetHendelse(
                behandlingId = jsonTree["behandlingId"].asText().let(UUID::fromString),
                ident = jsonTree["ident"].asText(),
                opprettet = LocalDateTime.parse(jsonTree["opprettet"].asText()),
                type = jsonTree["type"].asText().let(BehandlingType::valueOf),
                utførtAv = utførtAv,
            )
        }
    }
}
