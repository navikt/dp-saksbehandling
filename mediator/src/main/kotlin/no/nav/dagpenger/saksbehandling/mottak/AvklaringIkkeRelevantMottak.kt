package no.nav.dagpenger.saksbehandling.mottak

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.hendelser.IkkeRelevantAvklaringHendelse
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

internal class AvklaringIkkeRelevantMottak(
    rapidsConnection: RapidsConnection,
    private val oppgaveMediator: OppgaveMediator,
) : River.PacketListener {
    companion object {
        private val logger = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall")
        val rapidFilter: River.() -> Unit = {
            validate { it.demandValue("@event_name", "avklaring_lukket") }
            validate { it.requireKey("ident", "kode", "behandlingId") }
            validate { it.interestedIn("avklaringId") }
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
        val avklaringId = packet["avklaringId"].asText()
        val ident = packet["ident"].asText()
        val ikkeRelevantEmneknagg = packet.ikkeRelevantEmneknagg()

        withLoggingContext(
            "behandlingId" to "$behandlingId",
            "avklaringId" to avklaringId,
        ) {
            logger.info { "Mottok avklaring_lukket hendelse for behandlingId=$behandlingId og avklaringId=$avklaringId" }
            val hendelse =
                IkkeRelevantAvklaringHendelse(
                    ident = ident,
                    behandlingId = behandlingId,
                    ikkeRelevantEmneknagg = ikkeRelevantEmneknagg,
                )
            sikkerlogg.info { "Mottok avklaring_lukket hendelse: $hendelse" }
            oppgaveMediator.fjernEmneknagg(hendelse)
        }
    }
}

private fun JsonMessage.ikkeRelevantEmneknagg(): String = this["kode"].asText()
