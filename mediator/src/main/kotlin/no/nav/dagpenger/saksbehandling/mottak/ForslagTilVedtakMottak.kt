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
            validate { it.interestedIn("fastsatt", "vilkår", "opplysninger") }
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

            packet["vilkår"].elements().forEach { vilkårNode ->
                sikkerlogg.info { "Forslag til vedtak - vilkår: [${vilkårNode["navn"].asText()}] - [${vilkårNode["status"].asText()}]" }
            }

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

    private val JsonMessage.avslagEmneknagger: Set<String>
        get() {
            val mutableEmneknagger = mutableSetOf<String>()
            if (this["vilkår"]
                    .map { Pair(it["navn"].asText(), it["status"].asText()) }
                    .any { (navn, status) -> navn == "Oppfyller kravet til minsteinntekt eller verneplikt" && status == "IkkeOppfylt" }
            ) {
                mutableEmneknagger.add("Avslag minsteinntekt")
            }
            if (this["vilkår"]
                    .map { Pair(it["navn"].asText(), it["status"].asText()) }
                    .any { (navn, status) -> navn == "Krav til tap av arbeidsinntekt" && status == "IkkeOppfylt" }
            ) {
                mutableEmneknagger.add("Avslag arbeidsinntekt")
            }
            if (this["vilkår"]
                    .map { Pair(it["navn"].asText(), it["status"].asText()) }
                    .any { (navn, status) -> navn == "Tap av arbeidstid er minst terskel" && status == "IkkeOppfylt" }
            ) {
                mutableEmneknagger.add("Avslag arbeidstid")
            }
            if (this["vilkår"]
                    .map { Pair(it["navn"].asText(), it["status"].asText()) }
                    .any { (navn, status) -> navn == "Oppfyller kravet til alder" && status == "IkkeOppfylt" }
            ) {
                mutableEmneknagger.add("Avslag alder")
            }
            if (this["vilkår"]
                    .map { Pair(it["navn"].asText(), it["status"].asText()) }
                    .any { (navn, status) -> navn == "Mottar ikke andre fulle ytelser" && status == "IkkeOppfylt" }
            ) {
                mutableEmneknagger.add("Avslag andre ytelser")
            }
            if (this["vilkår"]
                    .map { Pair(it["navn"].asText(), it["status"].asText()) }
                    .any { (navn, status) -> navn == "Oppfyller kravet til medlemskap" && status == "IkkeOppfylt" }
            ) {
                mutableEmneknagger.add("Avslag medlemskap")
            }
            if (this["vilkår"]
                    .map { Pair(it["navn"].asText(), it["status"].asText()) }
                    .any { (navn, status) -> navn == "Er medlemmet ikke påvirket av streik eller lock-out?" && status == "IkkeOppfylt" }
            ) {
                mutableEmneknagger.add("Avslag streik")
            }
            if (this["vilkår"]
                    .map { Pair(it["navn"].asText(), it["status"].asText()) }
                    .any { (navn, status) -> navn == "Oppfyller kravet til opphold i Norge" && status == "IkkeOppfylt" }
            ) {
                mutableEmneknagger.add("Avslag opphold utland")
            }
            if (this["vilkår"]
                    .map { Pair(it["navn"].asText(), it["status"].asText()) }
                    .any { (navn, status) -> navn == "Krav til arbeidssøker" && status == "IkkeOppfylt" }
            ) {
                mutableEmneknagger.add("Avslag reell arbeidssøker")
            }
            if (this["vilkår"]
                    .map { Pair(it["navn"].asText(), it["status"].asText()) }
                    .any { (navn, status) -> navn == "Registrert som arbeidssøker på søknadstidspunktet" && status == "IkkeOppfylt" }
            ) {
                mutableEmneknagger.add("Avslag ikke registrert")
            }
            if (this["vilkår"]
                    .map { Pair(it["navn"].asText(), it["status"].asText()) }
                    .any { (navn, status) -> navn == "Oppfyller krav til ikke utestengt" && status == "IkkeOppfylt" }
            ) {
                mutableEmneknagger.add("Avslag utestengt")
            }
            if (this["vilkår"]
                    .map { Pair(it["navn"].asText(), it["status"].asText()) }
                    .any { (navn, status) -> navn == "Krav til utdanning eller opplæring" && status == "IkkeOppfylt" }
            ) {
                mutableEmneknagger.add("Avslag utdanning")
            }
            if (mutableEmneknagger.isEmpty()) {
                mutableEmneknagger.add("Avslag")
            }
            return mutableEmneknagger.toSet()
        }

    private val JsonMessage.rettighetEmneknagg: Set<String>
        get() {
            val mutableEmneknagger = mutableSetOf<String>()
            if (this["fastsatt"]["kvoter"]
                    .map { Pair(it["navn"].asText(), it["verdi"].asInt()) }
                    .any { (navn, antallUker) -> navn == "Verneplikt" && antallUker > 0 }
            ) {
                mutableEmneknagger.add("Innvilgelse verneplikt")
            } else {
                if (this["opplysninger"]
                        .map { Pair(it["navn"].asText(), it["verdi"].asBoolean()) }
                        .any { (navn, harRettighet) -> navn == "Har rett til ordinære dagpenger" && harRettighet }
                ) {
                    mutableEmneknagger.add("Innvilgelse ordinær")
                }

                if (this["opplysninger"]
                        .map { Pair(it["navn"].asText(), it["verdi"].asBoolean()) }
                        .any { (navn, harRettighet) -> navn == "Har rett til dagpenger under permittering" && harRettighet }
                ) {
                    mutableEmneknagger.add("Innvilgelse permittering")
                }
                if (this["opplysninger"]
                        .map { Pair(it["navn"].asText(), it["verdi"].asBoolean()) }
                        .any {
                                (navn, harRettighet) ->
                            navn == "Har rett til dagpenger under permittering i fiskeforedlingsindustri" && harRettighet
                        }
                ) {
                    mutableEmneknagger.add("Innvilgelse permittering fisk")
                }
                if (this["opplysninger"]
                        .map { Pair(it["navn"].asText(), it["verdi"].asBoolean()) }
                        .any { (navn, harRettighet) -> navn == "Har rett til dagpenger etter konkurs" && harRettighet }
                ) {
                    mutableEmneknagger.add("Innvilgelse etter konkurs")
                }
            }
            if (mutableEmneknagger.isEmpty()) {
                mutableEmneknagger.add("Innvilgelse")
            }
            return mutableEmneknagger.toSet()
        }

    private fun JsonMessage.emneknagger(): Set<String> {
        when (this.utfall.asBoolean()) {
            true -> return this.rettighetEmneknagg
            false -> return this.avslagEmneknagger
        }
    }
}
