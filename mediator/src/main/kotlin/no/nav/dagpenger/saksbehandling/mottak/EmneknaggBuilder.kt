package no.nav.dagpenger.saksbehandling.mottak

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import no.nav.dagpenger.saksbehandling.Emneknagg
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
import no.nav.dagpenger.saksbehandling.mottak.OpplysningTyper.RETTIGHET_DAGPEGNER_UNDER_PERMITTERING
import no.nav.dagpenger.saksbehandling.mottak.OpplysningTyper.RETTIGHET_DAGPENGER_ETTER_KONKURS
import no.nav.dagpenger.saksbehandling.mottak.OpplysningTyper.RETTIGHET_DAGPENGER_ETTER_VERNEPLIKT
import no.nav.dagpenger.saksbehandling.mottak.OpplysningTyper.RETTIGHET_DAGPENGER_UNDER_PERMITTERING_I_FISKEFOREDLINGSINDUSTRI
import no.nav.dagpenger.saksbehandling.mottak.OpplysningTyper.RETTIGHET_ORDINÆRE_DAGPENGER

private val logger = KotlinLogging.logger { }
private val sikkerLogg = KotlinLogging.logger("tjenestekall")

class EmneknaggBuilder(
    json: String,
) {
    private val objectMapper =
        ObjectMapper().also {
            it.registerModule(JavaTimeModule())
        }

    private val jsonNode =
        try {
            objectMapper.readTree(json)
        } catch (e: Exception) {
            logger.error(e) { "Kunne ikke parse json: ${e.message}" }
            sikkerLogg.error { "Kunne ikke parse json: $json" }
            throw IllegalArgumentException("Kunne ikke parse json: $e")
        }.also {
            require(it.isObject) {
                sikkerLogg.error { "Forventet at json er et objekt: $it" }
            }
        }

    private val opplysningNode =
        jsonNode["opplysninger"].also {
            require(it.isPresent() && it.isArray) {
                sikkerLogg.error { "Kunne ikke parse opplysninger: $it" }
                "Forventet at opplysninger er en liste"
            }
        }

    private val rettighetsPerioder =
        jsonNode["rettighetsperioder"].also {
            require(it.isPresent() && it.isArray) {
                sikkerLogg.error { "Kunne ikke parse rettighetsperioder: $it" }
                "Forventet at rettighetsperioder er en liste"
            }
        }

    private val behandletHendelse =
        jsonNode["behandletHendelse"].also {
            require(it.isPresent() && it.isObject) {
                sikkerLogg.error { "Kunne ikke parse behandletHendelse: $it" }
                "Forventet at behandletHendelse er et objekt"
            }
        }

    private val rettighetTilEmneknagg =
        mapOf(
            RETTIGHET_ORDINÆRE_DAGPENGER.opplysningTypeId to RETTIGHET_ORDINÆR.visningsnavn,
            RETTIGHET_DAGPENGER_ETTER_VERNEPLIKT.opplysningTypeId to RETTIGHET_VERNEPLIKT.visningsnavn,
            RETTIGHET_DAGPEGNER_UNDER_PERMITTERING.opplysningTypeId to RETTIGHET_PERMITTERT.visningsnavn,
            RETTIGHET_DAGPENGER_UNDER_PERMITTERING_I_FISKEFOREDLINGSINDUSTRI.opplysningTypeId to
                RETTIGHET_PERMITTERT_FISK.visningsnavn,
            RETTIGHET_DAGPENGER_ETTER_KONKURS.opplysningTypeId to RETTIGHET_KONKURS.visningsnavn,
        )

    private val rettighetOpplysningId = rettighetTilEmneknagg.keys

    private val avslagTilEmneknagg =
        mapOf(
            OpplysningTyper.OPPFYLLER_KRAV_TIL_MINSTEINNTEKT.opplysningTypeId to AVSLAG_MINSTEINNTEKT.visningsnavn,
            OpplysningTyper.KRAV_TIL_TAP_AV_ARBEIDSINNTEKT.opplysningTypeId to AVSLAG_ARBEIDSINNTEKT.visningsnavn,
            OpplysningTyper.TAP_AV_ARBEIDSTID_ER_MINST_TERSKEL.opplysningTypeId to AVSLAG_ARBEIDSTID.visningsnavn,
            OpplysningTyper.KRAV_TIL_ALDER.opplysningTypeId to AVSLAG_ALDER.visningsnavn,
            OpplysningTyper.IKKE_FULLE_YTELSER.opplysningTypeId to AVSLAG_ANDRE_YTELSER.visningsnavn,
            OpplysningTyper.OPPFYLLER_MEDLEMSKAP.opplysningTypeId to AVSLAG_MEDLEMSKAP.visningsnavn,
            OpplysningTyper.IKKE_PÅVIRKET_AV_STREIK_ELLER_LOCKOUT.opplysningTypeId to AVSLAG_STREIK.visningsnavn,
            OpplysningTyper.OPPFYLLER_KRAVET_OPPHOLD.opplysningTypeId to AVSLAG_OPPHOLD_UTLAND.visningsnavn,
            OpplysningTyper.KRAV_TIL_ARBEIDSSØKER.opplysningTypeId to AVSLAG_REELL_ARBEIDSSØKER.visningsnavn,
            OpplysningTyper.OPPYLLER_KRAV_TIL_REGISTRERT_ARBEIDSSØKER.opplysningTypeId to AVSLAG_IKKE_REGISTRERT.visningsnavn,
            OpplysningTyper.OPPFYLLER_KRAV_TIL_IKKE_UTESTENGT.opplysningTypeId to AVSLAG_UTESTENGT.visningsnavn,
            OpplysningTyper.KRAV_TIL_UTDANNING_ELLER_OPPLÆRING.opplysningTypeId to AVSLAG_UTDANNING.visningsnavn,
        )

    private val avslagOpplysningId = avslagTilEmneknagg.keys

    fun bygg(): Set<String> {
        return buildSet {
            addAll(rettighetEmneknagger())
            when (utfall()) {
                true -> add(INNVILGELSE.visningsnavn)
                false -> {
                    add(Emneknagg.Regelknagg.AVSLAG.visningsnavn)
                    addAll(avslagEmneknagger())
                }
            }
            val hendelseType = behandletHendelse["type"].asText()
            when (hendelseType) {
                "Meldekort" -> add(Emneknagg.BehandletHendelseType.MELDEKORT.visningsnavn)
                "Manuell" -> add(Emneknagg.BehandletHendelseType.MANUELL.visningsnavn)
            }
        }
    }

    private fun avslagEmneknagger(): Set<String> {
        return opplysningNode.filter { it["opplysningTypeId"].asUUID() in avslagOpplysningId }
            .filter { it["perioder"].any { periode -> !periode["verdi"].boolskVerdi() } }
            .mapNotNull { avslagTilEmneknagg[it["opplysningTypeId"].asUUID()] }
            .toSet()
    }

    private fun rettighetEmneknagger(): Set<String> {
        return opplysningNode.filter { it["opplysningTypeId"].asUUID() in rettighetOpplysningId }
            .filter { it["perioder"].any { periode -> periode["verdi"].boolskVerdi() } }
            .mapNotNull { rettighetTilEmneknagg[it["opplysningTypeId"].asUUID()] }
            .toSet()
    }

    private fun utfall(): Boolean {
        val utfall = rettighetsPerioder.any { it["harRett"].asBoolean() }
        sikkerLogg.info {
            val behadlingId = jsonNode["behandlingId"].asText()
            withLoggingContext("Id" to behadlingId) {
                "BehandlingId: $behadlingId har utfall: $utfall med rettighetsPerioder: $rettighetsPerioder"
            }
        }
        return utfall
    }

    private fun JsonNode.boolskVerdi(): Boolean {
        return this["datatype"].asText() == "boolsk" && this["verdi"].asBoolean()
    }

    private fun JsonNode?.isPresent(): Boolean {
        if (this == null) return false
        return !(this.isMissingNode || this.isNull)
    }
}
