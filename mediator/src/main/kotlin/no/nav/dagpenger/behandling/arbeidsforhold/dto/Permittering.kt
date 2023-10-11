package no.nav.dagpenger.behandling.arbeidsforhold.dto

data class Permittering(
    val startdato: String? = null,
    val sluttdato: String? = null,
    // Id fra opplysningspliktig
    val id: String? = null,
    // Prosent for permittering
    val prosent: Double? = null,
    // Permitteringstype (kodeverk: PermisjonsOgPermitteringsBeskrivelse)
    val type: Kodeverksentitet? = null,
)
