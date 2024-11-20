package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
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
            validate { it.demandValue("@event_name", "forslag_til_vedtak") }
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
        if (this["utfall"].isMissingOrNull()) {
            logger.warn { "Fant ikke utfall. Lager ingen emneknagger." }
            return emptySet()
        }
        when (this["utfall"].asBoolean()) {
            true -> return setOf("Innvilgelse")
            false -> {
                if (this["harAvklart"].isMissingOrNull()) {
                    logger.warn { "Fant ikke harAvklart men utfallet er avslag, lager emneknagg Avslag." }
                    return setOf("Avslag")
                }
                if (this["harAvklart"].asText() == "Krav til minsteinntekt") {
                    return setOf("Avslag minsteinntekt")
                } else {
                    logger.warn {
                        "Klarte ikke sette emneknagg for ukjent verdi i harAvklart når uftallet er avslag. " +
                            "Element harAvklart har verdi: ${this["harAvklart"].asText()}."
                    }
                    return setOf("Avslag")
                }
            }
        }
    }
}
