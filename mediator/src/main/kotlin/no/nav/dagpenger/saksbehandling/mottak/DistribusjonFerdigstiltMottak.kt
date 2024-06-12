package no.nav.dagpenger.saksbehandling.mottak

import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.hendelser.UtsendingFerdigstiltHendelse
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

class DistribusjonFerdigstiltMottak(
    private val oppgaveMediator: OppgaveMediator,
    rapidsConnection: RapidsConnection,
) : River.PacketListener {
    companion object {
        private val logger = KotlinLogging.logger {}
        val rapidFilter: River.() -> Unit = {
            validate { it.demandValue("@event_name", "behov") }
            validate { it.requireKey("@løsning") }
            validate { it.requireKey("oppgaveId") }
            validate {
                it.requireAllOrAny(
                    "@behov",
                    listOf("DistribueringBehov"),
                )
            }
        }
    }

    init {
        River(rapidsConnection).apply(rapidFilter).register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val oppgaveId = packet["oppgaveId"].asUUID()
        logger.info { "Mottok distribusjon ferdigstilt for oppgave $oppgaveId" }
        oppgaveMediator.ferdigstillOppgave(utsendingFerdigstiltHendelse = UtsendingFerdigstiltHendelse(oppgaveId))
    }
}
