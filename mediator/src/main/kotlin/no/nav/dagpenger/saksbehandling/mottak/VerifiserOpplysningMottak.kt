package no.nav.dagpenger.saksbehandling.mottak

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.saksbehandling.Mediator
import no.nav.dagpenger.saksbehandling.hendelser.VerifiserOpplysningHendelse
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

internal class VerifiserOpplysningMottak(
    rapidsConnection: RapidsConnection,
    private val mediator: Mediator,
) : River.PacketListener {
    companion object {
        private val logger = KotlinLogging.logger {}
        val rapidFilter: River.() -> Unit = {
            validate { it.demandValue("@event_name", "verifiser_opplysning") }
            validate { it.requireKey("ident", "behandlingId") }
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

        withLoggingContext("behandlingId" to "$behandlingId") {
            mediator.behandle(
                VerifiserOpplysningHendelse(
                    behandlingId = behandlingId,
                    ident = ident,
                ),
            )
        }
    }
}
