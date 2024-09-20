package no.nav.dagpenger.saksbehandling.mottak

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

internal class ForslagTilVedtakMottak(
    rapidsConnection: RapidsConnection,
    private val oppgaveMediator: OppgaveMediator,
) : River.PacketListener {
    companion object {
        private val sikkerlogg = KotlinLogging.logger("tjenestekall")
        private val logger = KotlinLogging.logger {}

        const val AVKLARINGER = "avklaringer"

        val rapidFilter: River.() -> Unit = {
            validate { it.demandValue("@event_name", "forslag_til_vedtak") }
            validate { it.requireKey("ident", "søknadId", "behandlingId") }
            validate { it.interestedIn(AVKLARINGER) }
        }
    }

    init {
        River(rapidsConnection).apply(rapidFilter).register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val søknadId = packet["søknadId"].asUUID()
        val behandlingId = packet["behandlingId"].asUUID()
        val ident = packet["ident"].asText()
        val emneknagger = packet.emneknagger()

        withLoggingContext("søknadId" to "$søknadId", "behandlingId" to "$behandlingId") {
            logger.info { "Mottok forslag_til_vedtak hendelse for søknadId $søknadId og behandlingId $behandlingId" }
            val forslagTilVedtakHendelse =
                ForslagTilVedtakHendelse(
                    ident = ident,
                    søknadId = søknadId,
                    behandlingId = behandlingId,
                    emneknagger = emneknagger,
                )
            sikkerlogg.info { "Mottok forslag_til_vedtak hendelse: $forslagTilVedtakHendelse" }
            oppgaveMediator.settOppgaveKlarTilBehandling(forslagTilVedtakHendelse)
        }
    }
}
