package no.nav.dagpenger.saksbehandling

data class Opplysning(
    val navn: String,
    val verdi: String? = null,
    val dataType: String,
    val status: OpplysningStatus,
//    private val gyldigFraOgMed: ZonedDate,
//    private val gyldigTilOgMed: ZonedDate,
//    val kilde: String,
)

enum class OpplysningStatus {
    Hypotese,
    Faktum,
}
