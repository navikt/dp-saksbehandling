package no.nav.dagpenger.behandling.hendelser.mottak

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.behandling.Mediator
import no.nav.dagpenger.behandling.hendelser.VurderAvslagPåMinsteinntektHendelse
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

        val årsakerTilManuellBehandling =
            listOf(
                "mulig gjenopptak",
                "svangerskapsrelaterte sykepenger",
                "EØS-arbeid",
                "har hatt lukkede saker siste 8 uker",
                "det er inntekt neste kalendermåned",
                "mulige inntekter fra fangst og fisk",
                "jobbet utenfor Norge",
            )

        val rapidFilter: River.() -> Unit = {
            validate { it.requireKey("@id") }
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
        val søknadUUID = packet["søknad_uuid"].asUUID()
        val ident = packet["identer"].asText()
        val meldingsreferanseId = packet["@id"].asUUID()
        val årsakTilManuellBehandling = packet["seksjon_navn"].asText()

        withLoggingContext("søknadId" to "$søknadUUID") {
            loggManuellBehandling(årsakTilManuellBehandling)
            mediator.behandle(
                VurderAvslagPåMinsteinntektHendelse(
                    søknadUUID = søknadUUID,
                    meldingsreferanseId = meldingsreferanseId,
                    ident = ident,
                ),
            )
        }
    }

    private fun loggManuellBehandling(årsakTilManuellBehandling: String?) =
        logger.info(
            "Fått hendelse om manuell behandling ($årsakTilManuellBehandling) av avslag på minsteinntekt",
        )
}
