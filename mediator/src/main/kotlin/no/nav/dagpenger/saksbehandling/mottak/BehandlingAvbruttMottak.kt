package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingAvbruttHendelse

internal class BehandlingAvbruttMottak(
    rapidsConnection: RapidsConnection,
    private val oppgaveMediator: OppgaveMediator,
) : River.PacketListener {
    companion object {
        private val logger = KotlinLogging.logger {}
        val rapidFilter: River.() -> Unit = {
            precondition {
                it.requireValue("@event_name", "behandling_avbrutt")
                it.requireAny(key = "behandletHendelse.type", values = listOf("Søknad", "Meldekort", "Manuell", "Omgjøring"))
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
        val behandletHendelseId = packet["behandletHendelse"]["id"].asText()
        val behandlingId = packet["behandlingId"].asUUID()

        withLoggingContext("behandletHendelseId" to "$behandletHendelseId", "behandlingId" to "$behandlingId") {
            logger.info { "Mottok behandling_avbrutt hendelse" }
            oppgaveMediator.avbrytOppgave(
                BehandlingAvbruttHendelse(
                    behandlingId = behandlingId,
                    behandletHendelseId = behandletHendelseId,
                    behandletHendelseType = packet["behandletHendelse"]["type"].asText(),
                    ident = packet["ident"].asText(),
                ),
            )
        }
    }
}
