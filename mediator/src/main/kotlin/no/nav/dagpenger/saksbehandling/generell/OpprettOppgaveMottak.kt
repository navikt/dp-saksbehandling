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
import no.nav.dagpenger.saksbehandling.hendelser.OpprettGenerellOppgaveHendelse
import no.nav.dagpenger.saksbehandling.serder.objectMapper

private val logger = KotlinLogging.logger {}

internal class OpprettOppgaveMottak(
    rapidsConnection: RapidsConnection,
    private val generellOppgaveMediator: GenerellOppgaveMediator,
) : River.PacketListener {
    companion object {
        val rapidFilter: River.() -> Unit = {
            precondition {
                it.requireValue("@event_name", "opprett_oppgave")
            }
            validate {
                it.requireKey("ident", "emneknagg", "tittel")
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

        withLoggingContext("emneknagg" to hendelse.emneknagg) {
            logger.info { "Mottok opprett_oppgave hendelse med emneknagg ${hendelse.emneknagg}" }
            generellOppgaveMediator.taImot(hendelse)
        }
    }
}

private fun opprettGenerellOppgaveHendelseFraPacket(packet: JsonMessage): OpprettGenerellOppgaveHendelse =
    OpprettGenerellOppgaveHendelse(
        ident = packet["ident"].asText(),
        emneknagg = packet["emneknagg"].asText(),
        tittel = packet["tittel"].asText(),
        beskrivelse = packet["beskrivelse"].takeUnless { it.isMissingOrNull() }?.asText() ?: "",
        strukturertData = packet["strukturertData"].takeUnless { it.isMissingOrNull() }?.tilMap() ?: emptyMap(),
    )

private fun JsonNode.isMissingOrNull(): Boolean = this.isMissingNode || this.isNull

@Suppress("UNCHECKED_CAST")
private fun JsonNode.tilMap(): Map<String, Any> = objectMapper.convertValue(this, Map::class.java) as Map<String, Any>
