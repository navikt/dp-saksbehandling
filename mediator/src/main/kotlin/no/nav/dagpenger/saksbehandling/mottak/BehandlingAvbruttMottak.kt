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
import java.util.UUID

internal class BehandlingAvbruttMottak(
    rapidsConnection: RapidsConnection,
    private val oppgaveMediator: OppgaveMediator,
) : River.PacketListener {
    companion object {
        private val logger = KotlinLogging.logger {}
        val rapidFilter: River.() -> Unit = {
            precondition {
                it.requireValue("@event_name", "behandling_avbrutt")
                it.requireValue("behandletHendelse.type", "Søknad")
            }
            validate { it.requireKey("ident", "behandlingId") }
            validate { it.interestedIn("behandletHendelse") }
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
        val søknadId = packet.søknadId()
        val behandlingId = packet["behandlingId"].asUUID()
        withLoggingContext("søknadId" to "$søknadId", "behandlingId" to "$behandlingId") {
            logger.info { "Mottok behandling_avbrutt hendelse" }
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

private fun JsonMessage.søknadId(): UUID = this["behandletHendelse"]["id"].asUUID()
