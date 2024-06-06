package no.nav.dagpenger.saksbehandling.hendelser

import java.util.UUID

data class StartUtsendingHendelse(
    val oppgaveId: UUID,
    val behandlingId: UUID,
    val ident: String,
)
