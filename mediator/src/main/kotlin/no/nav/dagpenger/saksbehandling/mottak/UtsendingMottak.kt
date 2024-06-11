package no.nav.dagpenger.saksbehandling.mottak

import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.UtsendingMediator
import no.nav.dagpenger.saksbehandling.hendelser.StartUtsendingHendelse
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
