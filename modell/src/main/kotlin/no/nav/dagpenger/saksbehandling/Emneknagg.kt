package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.Emneknagg.EmneknaggKategori.AVBRUTT_GRUNN
import no.nav.dagpenger.saksbehandling.Emneknagg.EmneknaggKategori.PÅ_VENT
import no.nav.dagpenger.saksbehandling.Emneknagg.EmneknaggKategori.SØKNADSRESULTAT
import java.time.LocalDate

enum class Emneknagg(
    val visningsnavn: String,
    val kategori: EmneknaggKategori,
) {
    AVSLAG("Avslag", SØKNADSRESULTAT),
    INNVILGELSE("Innvilgelse", SØKNADSRESULTAT),
    GJENOPPTAK("Gjenopptak", SØKNADSRESULTAT),
    AVSLAG_MINSTEINNTEKT("Minsteinntekt", SØKNADSRESULTAT),
    AVSLAG_ARBEIDSINNTEKT("Arbeidsinntekt", SØKNADSRESULTAT),
    AVSLAG_ARBEIDSTID("Arbeidstid", SØKNADSRESULTAT),
    AVSLAG_ALDER("Alder", SØKNADSRESULTAT),
    AVSLAG_ANDRE_YTELSER("Andre ytelser", SØKNADSRESULTAT),
    AVSLAG_STREIK("Streik", SØKNADSRESULTAT),
    AVSLAG_OPPHOLD_UTLAND("Opphold utland", SØKNADSRESULTAT),
    AVSLAG_REELL_ARBEIDSSØKER("Reell arbeidssøker", SØKNADSRESULTAT),
    AVSLAG_IKKE_REGISTRERT("Ikke registrert", SØKNADSRESULTAT),
    AVSLAG_UTESTENGT("Utestengt", SØKNADSRESULTAT),
    AVSLAG_UTDANNING("Utdanning", SØKNADSRESULTAT),
    AVSLAG_MEDLEMSKAP("Medlemskap", SØKNADSRESULTAT),
    RETTIGHET_ORDINÆR("Ordinær", SØKNADSRESULTAT),
    RETTIGHET_VERNEPLIKT("Verneplikt", SØKNADSRESULTAT),
    RETTIGHET_PERMITTERT("Permittert", SØKNADSRESULTAT),
    RETTIGHET_PERMITTERT_FISK("Permittert fisk", SØKNADSRESULTAT),
    RETTIGHET_KONKURS("Konkurs", SØKNADSRESULTAT),

    AVVENT_SVAR("Avvent svar", PÅ_VENT),
    AVVENT_DOKUMENTASJON("Avvent dokumentasjon", PÅ_VENT),
    AVVENT_MELDEKORT("Avvent meldekort", PÅ_VENT),
    AVVENT_PERMITTERINGSÅRSAK("Avvent permitteringsårsak", PÅ_VENT),
    AVVENT_RAPPORTERINGSFRIST("Avvent rapporteringsfrist", PÅ_VENT),
    AVVENT_SVAR_PÅ_FORESPØRSEL("Sendt forespørsel", PÅ_VENT),
    AVVENT_ANNET("Utsatt annen årsak", PÅ_VENT),
    TIDLIGERE_UTSATT("Tidligere utsatt", PÅ_VENT),

    AVBRUTT_BEHANDLES_I_ARENA("Behandles i Arena", AVBRUTT_GRUNN),
    AVBRUTT_FLERE_SØKNADER("Flere søknader", AVBRUTT_GRUNN),
    AVBRUTT_TRUKKET_SØKNAD("Trukket søknad", AVBRUTT_GRUNN),
    AVBRUTT_ANNET("Annen avbruddsårsak", AVBRUTT_GRUNN),
    ;

    enum class EmneknaggKategori {
        RETTIGHET,
        GJENOPPTAK,
        SØKNADSRESULTAT,
        AVSLAGSGRUNN,
        AVBRUTT_GRUNN,
        PÅ_VENT,
    }

    data class Ettersending(
        private val mottatt: LocalDate = LocalDate.now(),
    ) {
        companion object {
            val fastTekst = "Ettersending"
        }

        val visningsnavn = "$fastTekst($mottatt)"
    }
}
