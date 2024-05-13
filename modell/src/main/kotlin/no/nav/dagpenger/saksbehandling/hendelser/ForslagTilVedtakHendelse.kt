package no.nav.dagpenger.saksbehandling.hendelser

import java.util.UUID

data class ForslagTilVedtakHendelse(
    val ident: String,
    val søknadId: UUID,
    val behandlingId: UUID,
    val emneknagger: Set<String> = emptySet(),
)
