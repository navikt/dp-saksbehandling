package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Aktør
import java.util.UUID

data class GodkjennBehandlingMedBrevIArena(
    val oppgaveId: UUID,
    val saksbehandlerToken: String,
    private val aktør: Aktør,
) : Hendelse(aktør)
