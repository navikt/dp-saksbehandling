package no.nav.dagpenger.saksbehandling

import java.util.UUID

data class NySak(
    val id: UUID = UUIDv7.ny(),
    val søknadId: UUID,
    val behandlinger: List<Behandling>,
)
