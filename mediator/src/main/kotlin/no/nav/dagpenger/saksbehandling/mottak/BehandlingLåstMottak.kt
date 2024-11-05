package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingLåstHendelse

internal class BehandlingLåstMottak(
    rapidsConnection: RapidsConnection,
    private val oppgaveMediator: OppgaveMediator,
) : River.PacketListener {
    companion object {
        private val logger = KotlinLogging.logger {}
        val rapidFilter: River.() -> Unit = {
            validate { it.demandValue("@event_name", "behandling_endret_tilstand") }
            validate { it.demandValue("forrigeTilstand", "ForslagTilVedtak") }
            validate { it.demandValue("gjeldendeTilstand", "Låst") }
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
        val ident = packet["ident"].asText()
        val behandlingId = packet["behandlingId"].asUUID()
        withLoggingContext("behandlingId" to "$behandlingId") {
            logger.info { "Mottok behandling_endret_tilstand hendelse med ny tilstand 'Låst' for behandlingId $behandlingId." }
            oppgaveMediator.settOppgaveKlarTilKontroll(
                BehandlingLåstHendelse(
                    behandlingId = behandlingId,
                    ident = ident,
                ),
            )
        }
    }
}
