package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.hendelser.Hendelse
import java.time.LocalDateTime
import java.util.UUID

sealed class Behandling() {
    abstract val behandlingId: UUID
    abstract val opprettet: LocalDateTime
    abstract val hendelse: Hendelse
    abstract val utløstAv: UtløstAvType
    abstract val oppgaveId: UUID?
}

data class KlageBehandling(
    override val behandlingId: UUID,
    override val opprettet: LocalDateTime,
    override val hendelse: Hendelse,
    override val oppgaveId: UUID? = null,
) : Behandling() {
    override val utløstAv: UtløstAvType = UtløstAvType.KLAGE

    companion object {
        fun rehydrer(
            behandlingId: UUID,
            opprettet: LocalDateTime,
            hendelse: Hendelse,
        ) = KlageBehandling(
            behandlingId = behandlingId,
            opprettet = opprettet,
            hendelse = hendelse,
        )
    }
}

data class RettTilDagpengerBehandling(
    override val behandlingId: UUID,
    override val opprettet: LocalDateTime,
    override val hendelse: Hendelse,
    override val utløstAv: UtløstAvType,
    override val oppgaveId: UUID? = null,
) : Behandling() {
    companion object {
        fun rehydrer(
            behandlingId: UUID,
            opprettet: LocalDateTime,
            hendelse: Hendelse,
            utløstAv: UtløstAvType,
        ) = RettTilDagpengerBehandling(
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
