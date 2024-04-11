package no.nav.dagpenger.saksbehandling.mottak

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.saksbehandling.Mediator
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

private val logger = KotlinLogging.logger {}

internal class VedtakFattetMottak(
    rapidsConnection: RapidsConnection,
    private val mediator: Mediator,
) : River.PacketListener {
    companion object {
        val rapidFilter: River.() -> Unit = {
            validate { it.demandValue("@event_name", "vedtak_fattet") }
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

        withLoggingContext("søknadId" to "$søknadId", "behandlingId" to "$behandlingId") {
            logger.info { "Mottok vedtak fattet hendelse for søknadId $søknadId og behandlingId $behandlingId" }
            mediator.avsluttBehandling(
                VedtakFattetHendelse(
                    behandlingId = behandlingId,
                    søknadId = søknadId,
                    ident = ident,
                ),
            )
        }
    }
}
