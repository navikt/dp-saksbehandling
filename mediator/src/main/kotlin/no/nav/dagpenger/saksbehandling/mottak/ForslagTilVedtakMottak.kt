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
import no.nav.dagpenger.saksbehandling.mottak.Emneknagg.Regelknagg.AVSLAG
import no.nav.dagpenger.saksbehandling.mottak.Emneknagg.Regelknagg.AVSLAG_ALDER
import no.nav.dagpenger.saksbehandling.mottak.Emneknagg.Regelknagg.AVSLAG_ANDRE_YTELSER
import no.nav.dagpenger.saksbehandling.mottak.Emneknagg.Regelknagg.AVSLAG_ARBEIDSINNTEKT
import no.nav.dagpenger.saksbehandling.mottak.Emneknagg.Regelknagg.AVSLAG_ARBEIDSTID
import no.nav.dagpenger.saksbehandling.mottak.Emneknagg.Regelknagg.AVSLAG_IKKE_REGISTRERT
import no.nav.dagpenger.saksbehandling.mottak.Emneknagg.Regelknagg.AVSLAG_MEDLEMSKAP
import no.nav.dagpenger.saksbehandling.mottak.Emneknagg.Regelknagg.AVSLAG_MINSTEINNTEKT
import no.nav.dagpenger.saksbehandling.mottak.Emneknagg.Regelknagg.AVSLAG_OPPHOLD_UTLAND
import no.nav.dagpenger.saksbehandling.mottak.Emneknagg.Regelknagg.AVSLAG_REELL_ARBEIDSSØKER
import no.nav.dagpenger.saksbehandling.mottak.Emneknagg.Regelknagg.AVSLAG_STREIK
import no.nav.dagpenger.saksbehandling.mottak.Emneknagg.Regelknagg.AVSLAG_UTDANNING
import no.nav.dagpenger.saksbehandling.mottak.Emneknagg.Regelknagg.AVSLAG_UTESTENGT
import no.nav.dagpenger.saksbehandling.mottak.Emneknagg.Regelknagg.INNVILGELSE
import no.nav.dagpenger.saksbehandling.mottak.Emneknagg.Regelknagg.RETTIGHET_KONKURS
import no.nav.dagpenger.saksbehandling.mottak.Emneknagg.Regelknagg.RETTIGHET_ORDINÆR
import no.nav.dagpenger.saksbehandling.mottak.Emneknagg.Regelknagg.RETTIGHET_PERMITTERT
import no.nav.dagpenger.saksbehandling.mottak.Emneknagg.Regelknagg.RETTIGHET_PERMITTERT_FISK
import no.nav.dagpenger.saksbehandling.mottak.Emneknagg.Regelknagg.RETTIGHET_VERNEPLIKT
import no.nav.dagpenger.saksbehandling.mottak.OpplysningTyper.RETTIGHET_DAGPEGNER_UNDER_PERMITTERING
import no.nav.dagpenger.saksbehandling.mottak.OpplysningTyper.RETTIGHET_DAGPENGER_ETTER_KONKURS
import no.nav.dagpenger.saksbehandling.mottak.OpplysningTyper.RETTIGHET_DAGPENGER_ETTER_VERNEPLIKT
import no.nav.dagpenger.saksbehandling.mottak.OpplysningTyper.RETTIGHET_DAGPENGER_UNDER_PERMITTERING_I_FISKEFOREDLINGSINDUSTRI
import no.nav.dagpenger.saksbehandling.mottak.OpplysningTyper.RETTIGHET_ORDINÆRE_DAGPENGER

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

    private fun JsonMessage.emneknagger(): Set<String> =
        buildSet {
            addAll(rettighetEmneknagg)
            when (utfall) {
                true -> add(INNVILGELSE.navn)
                false -> addAll(avslagEmneknagger)
            }
        }

    private val JsonMessage.utfall get() = this["fastsatt"].get("utfall").asBoolean()

    private val JsonMessage.avslagEmneknagger: Set<String>
        get() {
            val avslagsgrunner = mutableSetOf(AVSLAG.navn)

            val vilkårTilAvslagEmneknagg =
                mapOf(
                    "Oppfyller kravet til minsteinntekt eller verneplikt" to AVSLAG_MINSTEINNTEKT.navn,
                    "Oppfyller kravet til minsteinntekt" to AVSLAG_MINSTEINNTEKT.navn,
                    "Krav til tap av arbeidsinntekt" to AVSLAG_ARBEIDSINNTEKT.navn,
                    "Tap av arbeidstid er minst terskel" to AVSLAG_ARBEIDSTID.navn,
                    "Oppfyller kravet til alder" to AVSLAG_ALDER.navn,
                    "Mottar ikke andre fulle ytelser" to AVSLAG_ANDRE_YTELSER.navn,
                    "Oppfyller kravet til medlemskap" to AVSLAG_MEDLEMSKAP.navn,
                    "Er medlemmet ikke påvirket av streik eller lock-out?" to AVSLAG_STREIK.navn,
                    "Oppfyller kravet til opphold i Norge" to AVSLAG_OPPHOLD_UTLAND.navn,
                    "Krav til arbeidssøker" to AVSLAG_REELL_ARBEIDSSØKER.navn,
                    "Registrert som arbeidssøker på søknadstidspunktet" to AVSLAG_IKKE_REGISTRERT.navn,
                    "Oppfyller krav til ikke utestengt" to AVSLAG_UTESTENGT.navn,
                    "Krav til utdanning eller opplæring" to AVSLAG_UTDANNING.navn,
                )

            this["vilkår"].map { it["navn"].asText() to it["status"].asText() }
                .filter { (_, status) -> status == "IkkeOppfylt" }
                .mapNotNull { (navn, _) -> vilkårTilAvslagEmneknagg[navn] }
                .forEach { avslagsgrunner.add(it) }

            return avslagsgrunner
        }

    private val JsonMessage.rettighetEmneknagg: Set<String>
        get() {
            val rettighetTilEmneknagg =
                mapOf(
                    RETTIGHET_ORDINÆRE_DAGPENGER.opplysningTypeId to RETTIGHET_ORDINÆR.navn,
                    RETTIGHET_DAGPENGER_ETTER_VERNEPLIKT.opplysningTypeId to RETTIGHET_VERNEPLIKT.navn,
                    RETTIGHET_DAGPEGNER_UNDER_PERMITTERING.opplysningTypeId to RETTIGHET_PERMITTERT.navn,
                    RETTIGHET_DAGPENGER_UNDER_PERMITTERING_I_FISKEFOREDLINGSINDUSTRI.opplysningTypeId to RETTIGHET_PERMITTERT_FISK.navn,
                    RETTIGHET_DAGPENGER_ETTER_KONKURS.opplysningTypeId to RETTIGHET_KONKURS.navn,
                )

            return this["opplysninger"]
                .map { it["opplysningTypeId"].asUUID() to it["verdi"].asBoolean() }
                .filter { (_, harRettighet) -> harRettighet }
                .mapNotNull { (id, _) -> rettighetTilEmneknagg[id] }
                .toSet()
        }
}
