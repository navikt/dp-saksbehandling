package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.hendelser.Hendelse
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
import java.time.LocalDateTime
import java.util.UUID

data class Behandling(
    val behandlingId: UUID,
    val person: Person,
    val opprettet: LocalDateTime,
    val hendelse: Hendelse = TomHendelse,
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
