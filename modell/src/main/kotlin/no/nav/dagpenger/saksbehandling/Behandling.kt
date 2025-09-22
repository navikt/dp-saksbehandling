package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.UtløstAvType.SØKNAD
import no.nav.dagpenger.saksbehandling.hendelser.Hendelse
import java.time.LocalDateTime
import java.util.UUID

data class Behandling(
    val behandlingId: UUID,
    val opprettet: LocalDateTime,
    val hendelse: Hendelse,
    val utløstAv: UtløstAvType = SØKNAD,
    val oppgaveId: UUID? = null,
) {
    companion object {
        fun rehydrer(
            behandlingId: UUID,
            opprettet: LocalDateTime,
            hendelse: Hendelse,
            utløstAv: UtløstAvType = SØKNAD,
        ) = Behandling(
            behandlingId = behandlingId,
            opprettet = opprettet,
            hendelse = hendelse,
            utløstAv = utløstAv,
        )
    }
}

enum class UtløstAvType {
    KLAGE,
    SØKNAD,
    MELDEKORT,
    MANUELL,
}
