package no.nav.dagpenger.saksbehandling.utsending.mottak

import mu.withLoggingContext
import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.mottak.asUUID
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator
import no.nav.dagpenger.saksbehandling.utsending.hendelser.StartUtsendingHendelse
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

class UtsendingMottak(rapidsConnection: RapidsConnection, private val utsendingMediator: UtsendingMediator) :
    River.PacketListener {
    companion object {
        val rapidFilter: River.() -> Unit = {
            validate { it.demandValue("@event_name", "start_utsending") }
            validate { it.requireKey("oppgaveId", "behandlingId", "ident", "sak") }
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
        val behandlingId = packet["behandlingId"].asUUID()
        val ident = packet["ident"].asText()
        val sak =
            Sak(
                id = packet["sak"]["id"].asText(),
                kontekst = packet["sak"]["kontekst"].asText(),
            )
       withLoggingContext(
            "oppgaveId" to "$oppgaveId",
            "behandlingId" to "$behandlingId",
            "sak" to sak.id,
        ) {
            utsendingMediator.mottaStartUtsending(
                StartUtsendingHendelse(
                    oppgaveId = oppgaveId,
                    behandlingId = behandlingId,
                    ident = ident,
                    sak = sak,
                ),
            )
        }
    }
}
