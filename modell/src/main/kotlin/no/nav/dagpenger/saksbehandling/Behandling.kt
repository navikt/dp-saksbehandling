package no.nav.dagpenger.saksbehandling

import java.time.LocalDateTime
import java.util.UUID

data class Behandling(
    val behandlingId: UUID,
    val person: Person,
    val opprettet: LocalDateTime,
) {
    companion object {
        fun rehydrer(
            behandlingId: UUID,
            person: Person,
            opprettet: LocalDateTime,
        ) = Behandling(
            behandlingId = behandlingId,
            person = person,
            opprettet = opprettet,
        )
    }
}
