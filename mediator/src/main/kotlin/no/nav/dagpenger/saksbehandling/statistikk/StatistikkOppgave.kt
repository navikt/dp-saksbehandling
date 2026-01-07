package no.nav.dagpenger.saksbehandling.statistikk

import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.Oppgave
import java.time.LocalDateTime
import java.util.UUID

data class StatistikkOppgave constructor(
    val behandling: StatistikkBehandling,
    val sakId: UUID,
    val oppgaveTilstander: List<OppgaveTilstandEndring>,
    val personIdent: String,
) {
    constructor(
        behandling: Behandling,
        oppgave: Oppgave,
        sakId: UUID,
    ) : this(
        behandling = StatistikkBehandling(
            id = behandling.behandlingId,
            tidspunkt = behandling.opprettet,
            basertPåBehandling = null,
            utløstAv = UtløstAv(
                type = behandling.utløstAv.name,
                tidspunkt = behandling.opprettet //todo
            )
        ),
        sakId = sakId,
        oppgaveTilstander = oppgave.tilstandslogg.map {
            OppgaveTilstandEndring(
                tilstand = it.tilstand.name,
                tidspunkt = it.tidspunkt,
                saksbehandlerIdent = null, //todo
                beslutterIdent = null, //todo
            )
        },
        personIdent = oppgave.personIdent()
    )

    data class StatistikkBehandling(
        val id: UUID,
        val tidspunkt: LocalDateTime,
        val basertPåBehandling: UUID?,
        val utløstAv: UtløstAv,
    )

    data class UtløstAv(
        val type: String,
        val tidspunkt: LocalDateTime,
    )

    data class OppgaveTilstandEndring(
        val tilstand: String,
        val tidspunkt: LocalDateTime,
        val saksbehandlerIdent: String?,
        val beslutterIdent: String?,
    )
}

