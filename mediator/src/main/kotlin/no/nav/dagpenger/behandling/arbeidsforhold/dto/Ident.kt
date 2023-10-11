package no.nav.dagpenger.behandling.arbeidsforhold.dto

data class Ident(
    val ident: String? = null,
    val type: String? = null,
)

data class Identer(
    val identer: List<Ident>? = null,
    val type: String? = null,
)
