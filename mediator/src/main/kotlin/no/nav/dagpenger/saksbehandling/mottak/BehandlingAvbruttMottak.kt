package no.nav.dagpenger.saksbehandling.mottak

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.saksbehandling.Mediator
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingAvbruttHendelse
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

internal class BehandlingAvbruttMottak(
    rapidsConnection: RapidsConnection,
    private val mediator: Mediator,
) : River.PacketListener {

    companion object {
        val sikkerlogg = KotlinLogging.logger("tjenestekall")
        private val logger = KotlinLogging.logger {}
        val rapidFilter: River.() -> Unit = {
            validate { it.demandValue("@event_name", "behandling_avbrutt") }
            validate { it.requireKey("ident", "søknadId", "behandlingId") }
        }
    }

    init {
        River(rapidsConnection).apply(rapidFilter).register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val ident = packet["ident"].asText()
        val søknadId = packet["søknadId"].asUUID()
        val behandlingId = packet["behandlingId"].asUUID()
        withLoggingContext("søknadId" to "$søknadId", "behandlingId" to "$behandlingId") {
            logger.info { "Mottok behandling avbrutt hendelse for søknadId $søknadId og behandlingId $behandlingId" }
            mediator.avbrytOppgave(
                BehandlingAvbruttHendelse(
                    behandlingId = behandlingId,
                    søknadId = søknadId,
                    ident = ident,
                ),
            )
            logger.info { "Behandling avbrutt for søknadId $søknadId og behandlingId $behandlingId" }
        }
    }
}
