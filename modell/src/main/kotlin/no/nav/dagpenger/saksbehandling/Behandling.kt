package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.BehandlingType.RETT_TIL_DAGPENGER
import java.time.LocalDateTime
import java.util.UUID

data class Behandling(
    val behandlingId: UUID,
    val opprettet: LocalDateTime,
    val type: BehandlingType = RETT_TIL_DAGPENGER,
    val oppgaveId: UUID? = null,
) {
    companion object {
        fun rehydrer(
            behandlingId: UUID,
            opprettet: LocalDateTime,
            type: BehandlingType = RETT_TIL_DAGPENGER,
            oppgaveId: UUID? = null,
        ) = Behandling(
            behandlingId = behandlingId,
            opprettet = opprettet,
            type = type,
            oppgaveId = oppgaveId,
        )
    }
}

enum class BehandlingType {
    KLAGE,
    RETT_TIL_DAGPENGER,
    MELDEKORT,
}
