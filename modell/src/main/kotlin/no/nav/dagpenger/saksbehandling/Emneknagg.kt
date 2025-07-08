package no.nav.dagpenger.saksbehandling

class Emneknagg(val navn: String) {
    enum class Regelknagg(val visningsnavn: String) {
        AVSLAG("Avslag"),
        INNVILGELSE("Innvilgelse"),
        AVSLAG_MINSTEINNTEKT("Minsteinntekt"),
        AVSLAG_ARBEIDSINNTEKT("Arbeidsinntekt"),
        AVSLAG_ARBEIDSTID("Arbeidstid"),
        AVSLAG_ALDER("Alder"),
        AVSLAG_ANDRE_YTELSER("Andre ytelser"),
        AVSLAG_STREIK("Streik"),
        AVSLAG_OPPHOLD_UTLAND("Opphold utland"),
        AVSLAG_REELL_ARBEIDSSØKER("Reell arbeidssøker"),
        AVSLAG_IKKE_REGISTRERT("Ikke registrert"),
        AVSLAG_UTESTENGT("Utestengt"),
        AVSLAG_UTDANNING("Utdanning"),
        AVSLAG_MEDLEMSKAP("Medlemskap"),
        RETTIGHET_ORDINÆR("Ordinær"),
        RETTIGHET_VERNEPLIKT("Verneplikt"),
        RETTIGHET_PERMITTERT("Permittert"),
        RETTIGHET_PERMITTERT_FISK("Permittert fisk"),
        RETTIGHET_KONKURS("Konkurs"),
    }

    enum class PåVent(val visningsnavn: String) {
        AVVENT_SVAR("Avvent svar"),
        AVVENT_DOKUMENTASJON("Avvent dokumentasjon"),
        AVVENT_MELDEKORT("Avvent meldekort"),
        AVVENT_PERMITTERINGSÅRSAK("Avvent permitteringsårsak"),
        AVVENT_RAPPORTERINGSFRIST("Avvent rapporteringsfrist"),
        AVVENT_SVAR_PÅ_FORESPØRSEL("Sendt forespørsel"),
        AVVENT_ANNET("Utsatt annen årsak"),

        TIDLIGERE_UTSATT("Tidligere utsatt"),
    }

    enum class BehandletHendelseType(val visningsnavn: String) {
        MELDEKORT("Meldekortsberegning"),
        MANUELL("Manuell"),
    }
}
