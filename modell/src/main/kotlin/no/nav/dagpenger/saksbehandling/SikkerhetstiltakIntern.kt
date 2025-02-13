package no.nav.dagpenger.saksbehandling

import java.time.LocalDate

data class SikkerhetstiltakIntern(
    val type: String,
    val beskrivelse: String,
    val gyldigFom: LocalDate,
    val gyldigTom: LocalDate,
) {
    fun erGyldig(dato: LocalDate) = !dato.isBefore(gyldigFom) && !dato.isAfter(gyldigTom)
}
