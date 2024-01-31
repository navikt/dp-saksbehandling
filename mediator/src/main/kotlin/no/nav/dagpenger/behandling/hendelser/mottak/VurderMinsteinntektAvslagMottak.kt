package no.nav.dagpenger.behandling.hendelser.mottak

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.behandling.Mediator
import no.nav.dagpenger.behandling.serder.asUUID
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

internal class VurderMinsteinntektAvslagMottak(
    rapidsConnection: RapidsConnection,
    private val mediator: Mediator,
) : River.PacketListener {
    companion object {
        private val logger = KotlinLogging.logger {}

        val rapidFilter: River.() -> Unit = {
            validate { it.demandValue("@event_name", "manuell_behandling") }
            validate { it.requireKey("søknad_uuid", "seksjon_navn") }
        }
    }

    init {
        River(rapidsConnection).apply(rapidFilter).register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val søknadUUID = packet["søknad_uuid"].asUUID()
        val årsakTilManuellBehandling = packet["seksjon_navn"].asText()

        withLoggingContext("søknadId" to "$søknadUUID") {
            loggManuellBehandling(årsakTilManuellBehandling)
        }
    }

    private fun loggManuellBehandling(årsakTilManuellBehandling: String?) =
        logger.info(
            "Fått hendelse om manuell behandling ($årsakTilManuellBehandling) av avslag på minsteinntekt",
        )
}
