package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.hendelser.IkkeRelevantAvklaringHendelse

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
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
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
