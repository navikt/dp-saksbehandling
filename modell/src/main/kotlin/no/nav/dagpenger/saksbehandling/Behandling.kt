package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.BehandlingType.RETT_TIL_DAGPENGER
import no.nav.dagpenger.saksbehandling.hendelser.Hendelse
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
import java.time.LocalDateTime
import java.util.UUID

data class Behandling(
    val behandlingId: UUID,
    val person: Person,
    val opprettet: LocalDateTime,
    val hendelse: Hendelse = TomHendelse,
    val type: BehandlingType = RETT_TIL_DAGPENGER,
) {
    companion object {
        fun rehydrer(
            behandlingId: UUID,
            person: Person,
            opprettet: LocalDateTime,
            hendelse: Hendelse,
            type: BehandlingType = RETT_TIL_DAGPENGER,
        ) = Behandling(
            behandlingId = behandlingId,
            person = person,
            opprettet = opprettet,
            hendelse = hendelse,
            type = type,
        )
    }
}

enum class BehandlingType {
    KLAGE,
    RETT_TIL_DAGPENGER,
}
