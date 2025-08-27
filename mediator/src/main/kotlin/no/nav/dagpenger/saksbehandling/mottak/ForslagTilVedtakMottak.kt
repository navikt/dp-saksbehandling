package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.saksbehandling.Emneknagg.BehandletHendelseType.MANUELL
import no.nav.dagpenger.saksbehandling.Emneknagg.BehandletHendelseType.MELDEKORT
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg.AVSLAG
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg.AVSLAG_ALDER
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg.AVSLAG_ANDRE_YTELSER
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg.AVSLAG_ARBEIDSINNTEKT
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg.AVSLAG_ARBEIDSTID
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg.AVSLAG_IKKE_REGISTRERT
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg.AVSLAG_MEDLEMSKAP
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg.AVSLAG_MINSTEINNTEKT
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg.AVSLAG_OPPHOLD_UTLAND
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg.AVSLAG_REELL_ARBEIDSSØKER
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg.AVSLAG_STREIK
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg.AVSLAG_UTDANNING
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg.AVSLAG_UTESTENGT
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg.INNVILGELSE
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg.RETTIGHET_KONKURS
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg.RETTIGHET_ORDINÆR
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg.RETTIGHET_PERMITTERT
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg.RETTIGHET_PERMITTERT_FISK
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg.RETTIGHET_VERNEPLIKT
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
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
                it.requireAny(key = "behandletHendelse.type", values = listOf("Søknad", "Meldekort", "Manuell"))
            }
            validate { it.requireKey("ident", "behandlingId") }
            validate { it.interestedIn("utfall", "harAvklart") }
            validate { it.interestedIn("fastsatt", "vilkår", "opplysninger") }
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
        val skipSet =
            setOf(
                "0198a7c4-c69b-72d6-82e0-7740b98e35a9",
                "01987454-6f7d-73c7-aa06-689015f6665b",
                "019878f2-97f6-715b-b22c-88bf17c1dda2",
                "0198a7c4-c69b-72d6-82e0-7740b98e35a9",
                "019897e7-b2f6-7bc8-8373-0395d91842dd",
                "0198e625-44c6-7801-bc1b-75aa8b6c8038",
            )
        if (behandlingId.toString() in skipSet) {
            logger.info { "Skipper behandlingId: $behandlingId fra ForslagTilVedtakMottak" }
            return
        }
        withLoggingContext("Id" to "$behandletHendelseId", "behandlingId" to "$behandlingId") {
            logger.info { "Mottok forslag_til_vedtak hendelse" }
            val ident = packet["ident"].asText()
            val emneknagger = packet.emneknagger()
            val forslagTilVedtakHendelse =
                ForslagTilVedtakHendelse(
                    ident = ident,
                    behandletHendelseId = behandletHendelseId,
                    behandletHendelseType = packet["behandletHendelse"]["type"].asText(),
                    behandlingId = behandlingId,
                    emneknagger = emneknagger,
                )

            packet["vilkår"].elements().forEach { vilkårNode ->
                sikkerlogg.info { "Forslag til vedtak - vilkår: [${vilkårNode["navn"].asText()}] - [${vilkårNode["status"].asText()}]" }
            }

            sikkerlogg.info { "Mottok forslag_til_vedtak hendelse: $forslagTilVedtakHendelse" }
            oppgaveMediator.opprettEllerOppdaterOppgave(forslagTilVedtakHendelse)
        }
    }

    private fun JsonMessage.emneknagger(): Set<String> =
        buildSet {
            addAll(rettighetEmneknagg)
            when (utfall) {
                true -> add(INNVILGELSE.visningsnavn)
                false -> addAll(avslagEmneknagger)
            }
            when (behandletHendelseType) {
                "Meldekort" -> add(MELDEKORT.visningsnavn)
                "Manuell" -> add(MANUELL.visningsnavn)
            }
        }

    private val JsonMessage.utfall get() = this["fastsatt"].get("utfall").asBoolean()
    private val JsonMessage.behandletHendelseType get() = this["behandletHendelse"]["type"].asText()

    private val JsonMessage.avslagEmneknagger: Set<String>
        get() {
            val avslagsgrunner = mutableSetOf(AVSLAG.visningsnavn)

            val vilkårTilAvslagEmneknagg =
                mapOf(
                    "Oppfyller kravet til minsteinntekt eller verneplikt" to AVSLAG_MINSTEINNTEKT.visningsnavn,
                    "Oppfyller kravet til minsteinntekt" to AVSLAG_MINSTEINNTEKT.visningsnavn,
                    "Krav til tap av arbeidsinntekt" to AVSLAG_ARBEIDSINNTEKT.visningsnavn,
                    "Tap av arbeidstid er minst terskel" to AVSLAG_ARBEIDSTID.visningsnavn,
                    "Oppfyller kravet til alder" to AVSLAG_ALDER.visningsnavn,
                    "Mottar ikke andre fulle ytelser" to AVSLAG_ANDRE_YTELSER.visningsnavn,
                    "Oppfyller kravet til medlemskap" to AVSLAG_MEDLEMSKAP.visningsnavn,
                    "Er medlemmet ikke påvirket av streik eller lock-out?" to AVSLAG_STREIK.visningsnavn,
                    "Oppfyller kravet til opphold i Norge" to AVSLAG_OPPHOLD_UTLAND.visningsnavn,
                    "Krav til arbeidssøker" to AVSLAG_REELL_ARBEIDSSØKER.visningsnavn,
                    "Registrert som arbeidssøker på søknadstidspunktet" to AVSLAG_IKKE_REGISTRERT.visningsnavn,
                    "Oppfyller krav til ikke utestengt" to AVSLAG_UTESTENGT.visningsnavn,
                    "Krav til utdanning eller opplæring" to AVSLAG_UTDANNING.visningsnavn,
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
                    RETTIGHET_ORDINÆRE_DAGPENGER.opplysningTypeId to RETTIGHET_ORDINÆR.visningsnavn,
                    RETTIGHET_DAGPENGER_ETTER_VERNEPLIKT.opplysningTypeId to RETTIGHET_VERNEPLIKT.visningsnavn,
                    RETTIGHET_DAGPEGNER_UNDER_PERMITTERING.opplysningTypeId to RETTIGHET_PERMITTERT.visningsnavn,
                    RETTIGHET_DAGPENGER_UNDER_PERMITTERING_I_FISKEFOREDLINGSINDUSTRI.opplysningTypeId to
                        RETTIGHET_PERMITTERT_FISK.visningsnavn,
                    RETTIGHET_DAGPENGER_ETTER_KONKURS.opplysningTypeId to RETTIGHET_KONKURS.visningsnavn,
                )

            return this["opplysninger"]
                .map { it["opplysningTypeId"].asUUID() to it["verdi"].asBoolean() }
                .filter { (_, harRettighet) -> harRettighet }
                .mapNotNull { (id, _) -> rettighetTilEmneknagg[id] }
                .toSet()
        }
}
