package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.hendelser.Hendelse
import java.time.LocalDateTime
import java.util.UUID

data class Behandling(
    val behandlingId: UUID,
    val opprettet: LocalDateTime,
    val hendelse: Hendelse,
    val utløstAv: UtløstAvType,
    val oppgaveId: UUID? = null,
) {
    companion object {
        fun rehydrer(
            behandlingId: UUID,
            opprettet: LocalDateTime,
            hendelse: Hendelse,
            utløstAv: UtløstAvType,
        ) = Behandling(
            behandlingId = behandlingId,
            opprettet = opprettet,
            hendelse = hendelse,
            utløstAv = utløstAv,
        )
    }
}

enum class UtløstAvType(
    val applikasjon: Applikasjon,
) {
    SØKNAD(applikasjon = Applikasjon.DpBehandling),
    MELDEKORT(applikasjon = Applikasjon.DpBehandling),
    MANUELL(applikasjon = Applikasjon.DpBehandling),
    REVURDERING(applikasjon = Applikasjon.DpBehandling),
    INNSENDING(applikasjon = Applikasjon.DpSaksbehandling),
    KLAGE(applikasjon = Applikasjon.DpSaksbehandling),
    TILBAKEKREVING(applikasjon = Applikasjon.Tilbakekreving),
    GENERELL(applikasjon = Applikasjon.DpSaksbehandling),
}
