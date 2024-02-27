package no.nav.dagpenger.saksbehandling

data class Opplysning(
    private val navn: String,
    val verdi: String? = null,
    val dataType: String,
//    val datatype: String,
//    private val gyldigFraOgMed: LocalDate,
//    private val gyldigTilOgMed: LocalDate,
//    val kilde: String,
)
