package no.nav.dagpenger.saksbehandling

data class Opplysning(
    val navn: String,
    val verdi: String? = null,
    val dataType: DataType,
    val status: OpplysningStatus,
    val redigerbar: Boolean,
//    private val gyldigFraOgMed: ZonedDate,
//    private val gyldigTilOgMed: ZonedDate,
//    val kilde: String,
)

enum class OpplysningStatus {
    Hypotese,
    Faktum,
}

enum class DataType {
    String,
    Boolean,
    Int,
    Double,
    LocalDate,
}
