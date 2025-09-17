package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.BehandlingType.SØKNAD
import no.nav.dagpenger.saksbehandling.hendelser.Hendelse
import java.time.LocalDateTime
import java.util.UUID

data class Behandling(
    val behandlingId: UUID,
    val opprettet: LocalDateTime,
    val hendelse: Hendelse,
    val type: BehandlingType = SØKNAD,
    val oppgaveId: UUID? = null,
) {
    companion object {
        fun rehydrer(
            behandlingId: UUID,
            opprettet: LocalDateTime,
            hendelse: Hendelse,
            type: BehandlingType = SØKNAD,
        ) = Behandling(
            behandlingId = behandlingId,
            opprettet = opprettet,
            hendelse = hendelse,
            type = type,
        )
    }
}

enum class BehandlingType {
    KLAGE,
    SØKNAD,
    MELDEKORT,
    MANUELL,
}
