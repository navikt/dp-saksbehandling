package no.nav.dagpenger.saksbehandling.mottak

import java.util.UUID

data class VedtakFattetHendelse(
    val behandlingId: UUID,
    val søknadId: UUID,
    val ident: String,
)
