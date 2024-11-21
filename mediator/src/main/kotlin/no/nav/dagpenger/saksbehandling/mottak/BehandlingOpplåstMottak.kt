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
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpplåstHendelse

internal class BehandlingOpplåstMottak(
    rapidsConnection: RapidsConnection,
    private val oppgaveMediator: OppgaveMediator,
) : River.PacketListener {
    companion object {
        private val logger = KotlinLogging.logger {}
        val rapidFilter: River.() -> Unit = {
            precondition {
                it.requireValue("@event_name", "behandling_endret_tilstand")
                it.requireValue("forrigeTilstand", "Låst")
                it.requireValue("gjeldendeTilstand", "ForslagTilVedtak")
            }
            validate { it.requireKey("ident", "behandlingId") }
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
        val ident = packet["ident"].asText()
        val behandlingId = packet["behandlingId"].asUUID()
        withLoggingContext("behandlingId" to "$behandlingId") {
            logger.info { "Mottok behandling_endret_tilstand hendelse med ny tilstand 'ForslagTilVedtak' for behandlingId $behandlingId." }
            oppgaveMediator.settOppgaveUnderBehandling(
                BehandlingOpplåstHendelse(
                    behandlingId = behandlingId,
                    ident = ident,
                ),
            )
        }
    }
}
