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
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingAvbruttHendelse

internal class BehandlingAvbruttMottak(
    rapidsConnection: RapidsConnection,
    private val oppgaveMediator: OppgaveMediator,
) : River.PacketListener {
    companion object {
        private val logger = KotlinLogging.logger {}
        val rapidFilter: River.() -> Unit = {
            validate { it.demandValue("@event_name", "behandling_avbrutt") }
            validate { it.requireKey("ident", "søknadId", "behandlingId") }
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
        val søknadId = packet["søknadId"].asUUID()
        val behandlingId = packet["behandlingId"].asUUID()
        withLoggingContext("søknadId" to "$søknadId", "behandlingId" to "$behandlingId") {
            logger.info { "Mottok behandling_avbrutt hendelse for behandlingId $behandlingId og søknadId $søknadId" }
            oppgaveMediator.avbrytOppgave(
                BehandlingAvbruttHendelse(
                    behandlingId = behandlingId,
                    søknadId = søknadId,
                    ident = ident,
                ),
            )
        }
    }
}
