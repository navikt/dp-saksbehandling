package no.nav.dagpenger.saksbehandling.mottak

import no.nav.dagpenger.saksbehandling.UtsendingMediator
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

class UtsendingMottak(rapidsConnection: RapidsConnection, private val mediator: UtsendingMediator) :
    River.PacketListener {
    companion object {
        val rapidFilter: River.() -> Unit = {
            validate { it.demandValue("@event_name", "start_utsending") }
            validate { it.requireKey("oppgaveId") }
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
        mediator.startUtsending(oppgaveId)
    }
}
