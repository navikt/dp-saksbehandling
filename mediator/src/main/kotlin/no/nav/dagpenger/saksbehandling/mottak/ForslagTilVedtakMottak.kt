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
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse

internal class ForslagTilVedtakMottak(
    rapidsConnection: RapidsConnection,
    private val oppgaveMediator: OppgaveMediator,
) : River.PacketListener {
    companion object {
        private val sikkerlogg = KotlinLogging.logger("tjenestekall")
        private val logger = KotlinLogging.logger {}

        val rapidFilter: River.() -> Unit = {
            precondition {
                it.requireValue("@event_name", "forslag_til_vedtak")
            }
            validate { it.requireKey("ident", "søknadId", "behandlingId") }
            validate { it.interestedIn("utfall", "harAvklart") }
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
        val søknadId = packet["søknadId"].asUUID()
        val behandlingId = packet["behandlingId"].asUUID()

        withLoggingContext("søknadId" to "$søknadId", "behandlingId" to "$behandlingId") {
            logger.info { "Mottok forslag_til_vedtak hendelse for søknadId $søknadId og behandlingId $behandlingId" }
            val ident = packet["ident"].asText()
            val emneknagger = packet.emneknagger()
            val forslagTilVedtakHendelse =
                ForslagTilVedtakHendelse(
                    ident = ident,
                    søknadId = søknadId,
                    behandlingId = behandlingId,
                    emneknagger = emneknagger,
                )
            sikkerlogg.info { "Mottok forslag_til_vedtak hendelse: $forslagTilVedtakHendelse" }
            oppgaveMediator.settOppgaveKlarTilBehandling(forslagTilVedtakHendelse)
        }
    }

    private fun JsonMessage.emneknagger(): Set<String> {
        return try {
            when (this["utfall"].asBoolean()) {
                true -> setOf("Innvilgelse")
                false -> {
                    if (this["harAvklart"].asText() == "Krav til minsteinntekt") {
                        return setOf("Avslag minsteinntekt")
                    } else {
                        logger.warn {
                            "Klarte ikke sette emneknagg for Innvilgelse eller Avslag minsteinntekt. " +
                                "Element harAvklart har verdi: ${this["harAvklart"].asText()}."
                        }
                        return setOf("Avslag")
                    }
                }
            }
        } catch (e: NullPointerException) {
            logger.warn { "Fant ikke utfall eller harAvklart. Lager ingen emneknagger." }
            return emptySet()
        }
    }
}
