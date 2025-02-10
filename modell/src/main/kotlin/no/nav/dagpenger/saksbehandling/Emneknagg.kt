package no.nav.dagpenger.saksbehandling

sealed class Emneknagg {
    open fun skalOverskrives() = false
}

object PåVentEmneknagger : Emneknagg() {
    const val TIDLIGERE_UTSATT = "Tidligere utsatt"

    enum class PåVentÅrsak(val navn: String) {
        AVVENT_SVAR("Avvent svar"),
        AVVENT_DOKUMENTASJON("Avvent dokumentasjon"),
        AVVENT_MELDEKORT("Avvent meldekort"),
        AVVENT_RAPPORTERINGSFRIST("Avvent rapporteringsfrist"),
        AVVENT_SVAR_PÅ_FORESPØRSEL("Sendt forespørsel"),
    }
}

object RegelmotorEmneknagger : Emneknagg() {
    override fun skalOverskrives() = true

    enum class Utfall(val navn: String) {
        AVSLAG("Avslag"),
        INNVILGELSE("Innvilgelse"),
    }

    enum class Rettighet(val navn: String) {
        RETTIGHET_ORDINÆR("Ordinær"),
        RETTIGHET_VERNEPLIKT("Verneplikt"),
        RETTIGHET_PERMITTERT("Permittert"),
        RETTIGHET_PERMITTERT_FISK("Permittert fisk"),
        RETTIGHET_KONKURS("Konkurs"),
    }

    enum class AvslagÅrsak(val navn: String) {
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
    }
}
