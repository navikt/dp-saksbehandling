package no.nav.dagpenger.saksbehandling.oppfolging

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.saksbehandling.hendelser.OpprettOppfølgingHendelse
import no.nav.dagpenger.saksbehandling.serder.objectMapper
import tools.jackson.databind.JsonNode
import java.time.LocalDate

private val logger = KotlinLogging.logger {}

internal class OpprettOppgaveMottak(
    rapidsConnection: RapidsConnection,
    private val oppfølgingMediator: OppfølgingMediator,
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
                it.interestedIn("beskrivelse", "strukturertData", "frist")
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
        val hendelse = opprettOppfølgingHendelseFraPacket(packet)

        withLoggingContext("aarsak" to hendelse.aarsak) {
            logger.info { "Mottok opprett_oppgave hendelse med årsak ${hendelse.aarsak}" }
            oppfølgingMediator.taImot(hendelse)
        }
    }
}

private fun opprettOppfølgingHendelseFraPacket(packet: JsonMessage): OpprettOppfølgingHendelse =
    OpprettOppfølgingHendelse(
        ident = packet["ident"].asText(),
        aarsak = packet["emneknagg"].asText(),
        tittel = packet["tittel"].asText(),
        beskrivelse = packet["beskrivelse"].takeUnless { it.isMissingOrNull() }?.asText() ?: "",
        strukturertData = packet["strukturertData"].takeUnless { it.isMissingOrNull() }?.tilMap() ?: emptyMap(),
        frist = packet["frist"].takeUnless { it.isMissingOrNull() }?.asText()?.let { LocalDate.parse(it) },
    )

private fun JsonNode.isMissingOrNull(): Boolean = this.isMissingNode || this.isNull

@Suppress("UNCHECKED_CAST")
private fun JsonNode.tilMap(): Map<String, Any> = objectMapper.convertValue(this, Map::class.java) as Map<String, Any>
