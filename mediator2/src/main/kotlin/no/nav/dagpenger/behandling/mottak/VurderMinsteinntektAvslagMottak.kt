package no.nav.dagpenger.behandling.hendelser.mottak

import com.fasterxml.jackson.databind.JsonNode
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.behandling.Mediator
import no.nav.dagpenger.behandling.modell.hendelser.VurderAvslagPåMinsteinntektHendelse
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import java.util.UUID

internal class VurderMinsteinntektAvslagMottak(
    rapidsConnection: RapidsConnection,
    private val mediator: Mediator,
) : River.PacketListener {
    companion object {
        private val logger = KotlinLogging.logger {}

        const val MULIG_GJENOPPTAK = "mulig gjenopptak"
        const val SVANGERSKAPSRELATERTE_SYKEPENGER = "svangerskapsrelaterte sykepenger"
        const val EØS_ARBEID = "EØS-arbeid"
        const val LUKKEDE_SAKER_NYLIG = "har hatt lukkede saker siste 8 uker"
        const val INNTEKT_NESTE_KALENDERMÅNED = "det er inntekt neste kalendermåned"
        const val INNTEKT_FRA_FANGST_OG_FISKE = "mulige inntekter fra fangst og fisk"
        const val JOBB_UTENFOR_NORGE = "jobbet utenfor Norge"

        val årsakerTilManuellBehandling =
            listOf(
                MULIG_GJENOPPTAK,
                SVANGERSKAPSRELATERTE_SYKEPENGER,
                EØS_ARBEID,
                LUKKEDE_SAKER_NYLIG,
                INNTEKT_NESTE_KALENDERMÅNED,
                INNTEKT_FRA_FANGST_OG_FISKE,
                JOBB_UTENFOR_NORGE,
            )

        val rapidFilter: River.() -> Unit = {
            validate { it.demandValue("@event_name", "manuell_behandling") }
            validate { it.demandAny("seksjon_navn", årsakerTilManuellBehandling) }
            validate { it.requireKey("søknad_uuid", "seksjon_navn", "identer") }
        }
    }

    init {
        River(rapidsConnection).apply(rapidFilter).register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val søknadId = packet["søknad_uuid"].asUUID()
        val ident = packet.folkeregisterIdent()
        val årsakTilManuellBehandling = packet["seksjon_navn"].asText()

        withLoggingContext("søknadId" to "$søknadId") {
            loggManuellBehandling(årsakTilManuellBehandling)
            mediator.behandle(
                VurderAvslagPåMinsteinntektHendelse(
                    søknadId = søknadId,
                    ident = ident,
                ),
            )
        }
    }

    private fun loggManuellBehandling(årsakTilManuellBehandling: String?) =
        logger.info(
            "Fått hendelse om manuell behandling ($årsakTilManuellBehandling) av avslag på minsteinntekt",
        )

    private fun JsonMessage.folkeregisterIdent() = this["identer"].first { it["type"].asText() == "folkeregisterident" }["id"].asText()

    fun JsonNode.asUUID(): UUID = this.asText().let { UUID.fromString(it) }
}
