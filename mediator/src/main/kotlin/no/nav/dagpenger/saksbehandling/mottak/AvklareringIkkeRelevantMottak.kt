package no.nav.dagpenger.saksbehandling.mottak

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.hendelser.IkkeRelevantAvklaringHendelse
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

private val logger = KotlinLogging.logger {}

internal class AvklareringIkkeRelevantMottak(
    rapidsConnection: RapidsConnection,
    private val oppgaveMediator: OppgaveMediator,
) : River.PacketListener {
    companion object {
        val rapidFilter: River.() -> Unit = {
            validate { it.demandValue("@event_name", "AvklaringIkkeRelevant") }
            validate { it.requireKey("ident", "kode", "behandlingId") }
        }
    }

    init {
        River(rapidsConnection).apply(rapidFilter).register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val behandlingId = packet["behandlingId"].asUUID()
        val ident = packet["ident"].asText()
        val ikkeRelevantEmneknagg = packet.ikkeRelevantEmneknagg()

        withLoggingContext("behandlingId" to "$behandlingId") {
            val hendelse =
                IkkeRelevantAvklaringHendelse(
                    ident = ident,
                    behandlingId = behandlingId,
                    ikkeRelevantEmneknagg = ikkeRelevantEmneknagg,
                )
            logger.info { "Mottok hendelse om ikke relevant avklaring  $hendelse" }
            oppgaveMediator.fjernEmneknagg(hendelse)
        }
    }
}

private fun JsonMessage.ikkeRelevantEmneknagg(): String {
    return this["kode"].asText()
}
