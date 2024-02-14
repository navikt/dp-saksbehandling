package no.nav.dagpenger.saksbehandling.mottak

import java.util.UUID

data class BehandlingOpprettetHendelse(
    val søknadId: UUID,
    val behandlingId: UUID,
    val ident: String,
)
