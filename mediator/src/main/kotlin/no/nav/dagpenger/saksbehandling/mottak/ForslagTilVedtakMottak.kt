package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.utsending.IngenBehov.navn

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
            validate { it.interestedIn("fastsatt", "vilkår") }
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
            logger.info { "Mottok forslag_til_vedtak hendelse" }
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

    private val JsonMessage.forslagUtfall get() = this["utfall"]
    private val JsonMessage.fastsattUtfall get() = this["fastsatt"].get("utfall")
    private val JsonMessage.utfall
        get() =
            when (this.forslagUtfall.isMissingOrNull()) {
                true -> this.fastsattUtfall
                false -> this.forslagUtfall
            }

    private val JsonMessage.vilkårEmneknagg
        get() =
            if (this["harAvklart"].isMissingOrNull()) {
                if (this["vilkår"]
                        .map { Pair(it["navn"].asText(), it["status"].asText()) }
                        .any { (navn, status) -> navn == "Oppfyller kravet til minsteinntekt eller verneplikt" && status == "IkkeOppfylt" }
                ) {
                    setOf("Avslag minsteinntekt")
                } else {
                    setOf("Avslag")
                }
            } else {
                emptySet()
            }

    private val JsonMessage.harAvklartEmneknagg
        get() =
            if (this["vilkår"].isMissingOrNull()) {
                if (this["harAvklart"].isMissingOrNull()) {
                    logger.warn { "Fant ikke harAvklart men utfallet er avslag, lager emneknagg Avslag." }
                    setOf("Avslag")
                } else if (this["harAvklart"].asText() == "Krav til minsteinntekt") {
                    setOf("Avslag minsteinntekt")
                } else {
                    logger.warn {
                        "Klarte ikke sette emneknagg for ukjent verdi i harAvklart når uftallet er avslag. " +
                            "Element harAvklart har verdi: ${this["harAvklart"].asText()}."
                    }
                    setOf("Avslag")
                }
            } else {
                emptySet()
            }

    private fun JsonMessage.emneknagger(): Set<String> {
        when (this.utfall.asBoolean()) {
            true -> return setOf("Innvilgelse")
            false -> {
                if (this.vilkårEmneknagg.isNotEmpty()) {
                    return this.vilkårEmneknagg
                }
                return this.harAvklartEmneknagg
            }
        }
    }
}
