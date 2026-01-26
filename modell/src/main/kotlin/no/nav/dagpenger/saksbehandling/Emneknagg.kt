package no.nav.dagpenger.saksbehandling

import java.time.LocalDate

object Emneknagg {
    data class Ettersending(
        private val mottatt: LocalDate = LocalDate.now(),
    ) {
        val fastTekst = "Ettersending"
        val visningsnavn = "$fastTekst($mottatt)"
        val kategori = EmneknaggKategori.ETTERSENDING
    }

    enum class Regelknagg(
        val visningsnavn: String,
        val kategori: EmneknaggKategori,
    ) {
        AVSLAG("Avslag", EmneknaggKategori.SØKNADSRESULTAT),
        INNVILGELSE("Innvilgelse", EmneknaggKategori.SØKNADSRESULTAT),
        GJENOPPTAK("Gjenopptak", EmneknaggKategori.GJENOPPTAK),
        AVSLAG_MINSTEINNTEKT("Minsteinntekt", EmneknaggKategori.AVSLAGSGRUNN),
        AVSLAG_ARBEIDSINNTEKT("Arbeidsinntekt", EmneknaggKategori.AVSLAGSGRUNN),
        AVSLAG_ARBEIDSTID("Arbeidstid", EmneknaggKategori.AVSLAGSGRUNN),
        AVSLAG_ALDER("Alder", EmneknaggKategori.AVSLAGSGRUNN),
        AVSLAG_ANDRE_YTELSER("Andre ytelser", EmneknaggKategori.AVSLAGSGRUNN),
        AVSLAG_STREIK("Streik", EmneknaggKategori.AVSLAGSGRUNN),
        AVSLAG_OPPHOLD_UTLAND("Opphold utland", EmneknaggKategori.AVSLAGSGRUNN),
        AVSLAG_REELL_ARBEIDSSØKER("Reell arbeidssøker", EmneknaggKategori.AVSLAGSGRUNN),
        AVSLAG_IKKE_REGISTRERT("Ikke registrert", EmneknaggKategori.AVSLAGSGRUNN),
        AVSLAG_UTESTENGT("Utestengt", EmneknaggKategori.AVSLAGSGRUNN),
        AVSLAG_UTDANNING("Utdanning", EmneknaggKategori.AVSLAGSGRUNN),
        AVSLAG_MEDLEMSKAP("Medlemskap", EmneknaggKategori.AVSLAGSGRUNN),
        RETTIGHET_ORDINÆR("Ordinær", EmneknaggKategori.RETTIGHET),
        RETTIGHET_VERNEPLIKT("Verneplikt", EmneknaggKategori.RETTIGHET),
        RETTIGHET_PERMITTERT("Permittert", EmneknaggKategori.RETTIGHET),
        RETTIGHET_PERMITTERT_FISK("Permittert fisk", EmneknaggKategori.RETTIGHET),
        RETTIGHET_KONKURS("Konkurs", EmneknaggKategori.RETTIGHET),
    }

    enum class PåVent(
        val visningsnavn: String,
    ) {
        AVVENT_SVAR("Avvent svar"),
        AVVENT_DOKUMENTASJON("Avvent dokumentasjon"),
        AVVENT_MELDEKORT("Avvent meldekort"),
        AVVENT_PERMITTERINGSÅRSAK("Avvent permitteringsårsak"),
        AVVENT_RAPPORTERINGSFRIST("Avvent rapporteringsfrist"),
        AVVENT_SVAR_PÅ_FORESPØRSEL("Sendt forespørsel"),
        AVVENT_ANNET("Utsatt annen årsak"),
        TIDLIGERE_UTSATT("Tidligere utsatt"),
        ;

        val kategori = EmneknaggKategori.PÅ_VENT
    }

    enum class AvbrytBehandling(
        val visningsnavn: String,
    ) {
        AVBRUTT_BEHANDLES_I_ARENA("Behandles i Arena"),
        AVBRUTT_FLERE_SØKNADER("Flere søknader"),
        AVBRUTT_TRUKKET_SØKNAD("Trukket søknad"),
        AVBRUTT_ANNET("Annen avbruddsårsak"),
        ;

        val kategori = EmneknaggKategori.AVBRUTT_GRUNN
    }
}

fun hentEmneknaggKategori(visningsnavn: String): EmneknaggKategori {
    if (visningsnavn.startsWith("Ettersending")) {
        return EmneknaggKategori.ETTERSENDING
    }

    Emneknagg.Regelknagg.entries.find { it.visningsnavn == visningsnavn }?.let {
        return it.kategori
    }

    Emneknagg.PåVent.entries.find { it.visningsnavn == visningsnavn }?.let {
        return EmneknaggKategori.PÅ_VENT
    }

    Emneknagg.AvbrytBehandling.entries.find { it.visningsnavn == visningsnavn }?.let {
        return EmneknaggKategori.AVBRUTT_GRUNN
    }

    return EmneknaggKategori.UDEFINERT
}

enum class EmneknaggKategori {
    RETTIGHET,
    GJENOPPTAK,
    SØKNADSRESULTAT,
    AVSLAGSGRUNN,
    AVBRUTT_GRUNN,
    PÅ_VENT,
    ETTERSENDING,
    UDEFINERT,
}
