package no.nav.dagpenger.saksbehandling

import java.time.ZonedDateTime
import java.util.UUID

data class Behandling(
    val behandlingId: UUID,
    val person: Person,
    val opprettet: ZonedDateTime,
) {
    companion object {
        fun rehydrer(
            behandlingId: UUID,
            person: Person,
            opprettet: ZonedDateTime,
        ) = Behandling(
            behandlingId = behandlingId,
            person = person,
            opprettet = opprettet,
        )
    }
}
