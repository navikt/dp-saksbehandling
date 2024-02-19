package no.nav.dagpenger.saksbehandling.hendelser

import java.util.UUID

data class SøknadsbehandlingOpprettetHendelse(
    val søknadId: UUID,
    val behandlingId: UUID,
    val ident: String,
)
