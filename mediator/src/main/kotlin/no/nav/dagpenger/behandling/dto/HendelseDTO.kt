package no.nav.dagpenger.behandling.dto

internal data class HendelseDTO(
    val id: String,
    val type: String,
    val tilstand: String,
)
