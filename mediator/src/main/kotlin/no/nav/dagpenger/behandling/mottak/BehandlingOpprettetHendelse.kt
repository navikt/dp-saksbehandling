package no.nav.dagpenger.behandling.mottak

import java.util.UUID

data class BehandlingOpprettetHendelse(
    val meldingsreferanseId: UUID = UUID.randomUUID(),
    val søknadId: UUID,
    val behandlingId: UUID,
    val ident: String,
)
