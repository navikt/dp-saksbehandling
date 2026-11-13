package no.nav.dagpenger.saksbehandling.mottak

import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg
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
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg.GJENOPPTAK
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg.INNVILGELSE
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg.RETTIGHET_EKSPORT
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg.RETTIGHET_ETABLERING
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg.RETTIGHET_KONKURS
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg.RETTIGHET_ORDINÆR
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg.RETTIGHET_PERMITTERT
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg.RETTIGHET_PERMITTERT_FISK
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg.RETTIGHET_VERNEPLIKT
import no.nav.dagpenger.saksbehandling.mottak.OpplysningTyper.RETTIGHET_DAGPEGNER_UNDER_PERMITTERING_SKAL_VURDERES
import no.nav.dagpenger.saksbehandling.mottak.OpplysningTyper.RETTIGHET_DAGPENGER_ETTER_VERNEPLIKT
import no.nav.dagpenger.saksbehandling.mottak.OpplysningTyper.RETTIGHET_DAGPENGER_UNDER_PERMITTERING_I_FISKEFOREDLINGSINDUSTRI_SKAL_VURDERES
import no.nav.dagpenger.saksbehandling.mottak.OpplysningTyper.RETTIGHET_EKSPORT_SKAL_VURDERES
import no.nav.dagpenger.saksbehandling.mottak.OpplysningTyper.RETTIGHET_ETABLERING_SKAL_VURDERES
import no.nav.dagpenger.saksbehandling.mottak.OpplysningTyper.RETTIGHET_LØNNSGARANTIMIDLER_ETTER_KONKURS
import no.nav.dagpenger.saksbehandling.mottak.OpplysningTyper.RETTIGHET_ORDINÆRE_DAGPENGER

class EmneknaggBuilder(
    json: String,
) {
    private val behandlingsResulutat = BehandlingsresultatMapper(json)

    fun bygg(): Set<String> =
        buildSet {
            addAll(rettighetEmneknagger())
            addAll(søknadEmneknagger())
            addAll(behandletHendelseEmneknagger())
        }

    private fun rettighetEmneknagger(): Set<String> =
        rettighetOpplysningIder
            .filter { behandlingsResulutat.finnesOpplysningMedVerdiTrue(it) }
            .mapNotNull { rettighetTilEmneknagg[it] }
            .toSet()

    private fun søknadEmneknagger(): Set<String> {
        val emneknagger = mutableSetOf<String>()
        if (behandlingsResulutat.erSøknad()) {
            if (behandlingsResulutat.søknadOmDagpengerInnvilges()) {
                emneknagger.add(INNVILGELSE.visningsnavn)
            } else {
                emneknagger.add(AVSLAG.visningsnavn)
                emneknagger.addAll(avslåtteVilkårEmneknagger())
            }

            if (behandlingsResulutat.erSøknadOmGjenopptak()) {
                emneknagger.add(GJENOPPTAK.visningsnavn)
            }
        }

        return emneknagger.toSet()
    }

    private fun behandletHendelseEmneknagger(): Set<String> =
        when (behandlingsResulutat.behandletHendelseType()) {
            "Arbeidssøkerperiode" -> setOf(Regelknagg.BEHANDLET_HENDELSE_TYPE_ARBEIDSSØKERPERIODE.visningsnavn)
            "Ferietillegg" -> setOf(Regelknagg.RETTIGHET_FERIETILLEGG.visningsnavn)
            "Samordning" -> setOf(Regelknagg.BEHANDLET_HENDELSE_TYPE_SAMORDNING.visningsnavn)
            else -> emptySet()
        }

    private val rettighetTilEmneknagg =
        mapOf(
            RETTIGHET_ORDINÆRE_DAGPENGER.opplysningTypeId to RETTIGHET_ORDINÆR.visningsnavn,
            RETTIGHET_DAGPENGER_ETTER_VERNEPLIKT.opplysningTypeId to RETTIGHET_VERNEPLIKT.visningsnavn,
            RETTIGHET_DAGPEGNER_UNDER_PERMITTERING_SKAL_VURDERES.opplysningTypeId to RETTIGHET_PERMITTERT.visningsnavn,
            RETTIGHET_DAGPENGER_UNDER_PERMITTERING_I_FISKEFOREDLINGSINDUSTRI_SKAL_VURDERES.opplysningTypeId to
                RETTIGHET_PERMITTERT_FISK.visningsnavn,
            RETTIGHET_LØNNSGARANTIMIDLER_ETTER_KONKURS.opplysningTypeId to RETTIGHET_KONKURS.visningsnavn,
            RETTIGHET_EKSPORT_SKAL_VURDERES.opplysningTypeId to RETTIGHET_EKSPORT.visningsnavn,
            RETTIGHET_ETABLERING_SKAL_VURDERES.opplysningTypeId to RETTIGHET_ETABLERING.visningsnavn,
        )

    private val rettighetOpplysningIder = rettighetTilEmneknagg.keys

    private val avslåtteVilkårTilEmneknagger =
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

    private val avslåtteVilkårOpplysningIder = avslåtteVilkårTilEmneknagger.keys

    private fun avslåtteVilkårEmneknagger(): Set<String> =
        avslåtteVilkårOpplysningIder
            .filter { behandlingsResulutat.finnesOpplysningMedVerdiFalse(it) }
            .mapNotNull { avslåtteVilkårTilEmneknagger[it] }
            .toSet()
}
