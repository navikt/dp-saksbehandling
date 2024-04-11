package no.nav.dagpenger.saksbehandling.hendelser

import java.util.UUID

data class VedtakFattetHendelse(
    val behandlingId: UUID,
    val søknadId: UUID,
    val ident: String,
)
