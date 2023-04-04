package no.nav.dagpenger.behandling.dto

internal data class BegrunnelseDTO(
    val kilde: String, // quiz, saksbehandler, dingseboms
    val tekst: String,
)
