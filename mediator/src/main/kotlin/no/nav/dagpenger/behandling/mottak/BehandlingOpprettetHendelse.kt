package no.nav.dagpenger.behandling.mottak

import java.util.UUID

data class BehandlingOpprettetHendelse(
    val søknadId: UUID,
    val behandlingId: UUID,
    val ident: String,
)
