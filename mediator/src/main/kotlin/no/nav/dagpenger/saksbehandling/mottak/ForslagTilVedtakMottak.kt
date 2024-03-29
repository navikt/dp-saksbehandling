package no.nav.dagpenger.saksbehandling.mottak

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.saksbehandling.Mediator
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import java.util.UUID

internal class ForslagTilVedtakMottak(
    rapidsConnection: RapidsConnection,
    private val mediator: Mediator,
) : River.PacketListener {

    companion object {
        private val logger = KotlinLogging.logger {}
        val rapidFilter: River.() -> Unit = {
            validate { it.demandValue("@event_name", "forslag_til_vedtak") }
            validate { it.requireKey("ident", "søknadId", "behandlingId") }
        }
    }

    init {
        River(rapidsConnection).apply(rapidFilter).register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val søknadId = packet["søknadId"].asUUID()
        val behandlingId = packet["behandlingId"].asUUID()
        val ident = packet["ident"].asText()

        withLoggingContext(loggingContext(søknadId, behandlingId)) {
            val forslagTilVedtakHendelse = ForslagTilVedtakHendelse(
                ident = ident,
                søknadId = søknadId,
                behandlingId = behandlingId,
            )
            logger.info { "Mottok hendelse om forslag til vedtak $forslagTilVedtakHendelse" }
            mediator.behandle(forslagTilVedtakHendelse)
        }
    }

    private fun loggingContext(søknadId: UUID, behandlingId: UUID) = mapOf("søknadId" to "$søknadId", "behandlingId" to "$behandlingId")
}
