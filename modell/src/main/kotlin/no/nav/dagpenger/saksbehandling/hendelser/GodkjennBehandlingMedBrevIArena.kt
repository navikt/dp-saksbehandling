package no.nav.dagpenger.saksbehandling.hendelser

import java.util.UUID

data class GodkjennBehandlingMedBrevIArena(
    val oppgaveId: UUID,
    val saksbehandlerToken: String,
)