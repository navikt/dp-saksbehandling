package no.nav.dagpenger.saksbehandling

import java.util.UUID

data class Opplysning(
    private val opplysningId: UUID,
    private val navn: String,
    val verdi: String? = null,
//    val opplysningstype: String,
//    val datatype: String,
//    private val gyldigFraOgMed: LocalDate,
//    private val gyldigTilOgMed: LocalDate,
//    val kilde: String,
)
