package no.nav.dagpenger.saksbehandling.generell

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.hendelser.OpprettGenerellOppgaveHendelse

private val logger = KotlinLogging.logger {}

internal class OpprettOppgaveMottak(
    rapidsConnection: RapidsConnection,
    private val oppgaveMediator: OppgaveMediator,
) : River.PacketListener {
    companion object {
        val rapidFilter: River.() -> Unit = {
            precondition {
                it.requireValue("@event_name", "opprett_oppgave")
            }
            validate {
                it.requireKey("ident", "oppgaveType", "tittel")
            }
            validate {
                it.interestedIn("beskrivelse", "strukturertData")
            }
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
        val hendelse = opprettGenerellOppgaveHendelseFraPacket(packet)

        withLoggingContext("oppgaveType" to hendelse.oppgaveType) {
            logger.info { "Mottok opprett_oppgave hendelse med type ${hendelse.oppgaveType}" }
            oppgaveMediator.håndter(hendelse)
        }
    }
}

private fun opprettGenerellOppgaveHendelseFraPacket(packet: JsonMessage): OpprettGenerellOppgaveHendelse =
    OpprettGenerellOppgaveHendelse(
        ident = packet["ident"].asText(),
        oppgaveType = packet["oppgaveType"].asText(),
        tittel = packet["tittel"].asText(),
        beskrivelse = packet["beskrivelse"].takeIf { !it.isMissingOrNull() }?.asText(),
        strukturertData = packet["strukturertData"].takeIf { !it.isMissingOrNull() },
    )

private fun JsonNode.isMissingOrNull(): Boolean = this.isMissingNode || this.isNull
