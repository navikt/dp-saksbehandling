package no.nav.dagpenger.saksbehandling.statistikk

import no.nav.dagpenger.saksbehandling.Configuration
import no.nav.dagpenger.saksbehandling.Oppgave
import java.time.LocalDateTime
import java.util.UUID

data class StatistikkOppgave(
    val behandling: StatistikkBehandling,
    val oppgaveId: UUID,
    val sakId: UUID,
    val personIdent: String,
    val saksbehandlerIdent: String?,
    val beslutterIdent: String?,
    val oppgaveTilstander: List<StatistikkOppgaveTilstandsendring>,
    val versjon: String = Configuration.versjon,
) {
    fun asMap(): Map<String, Any> =
        buildMap {
            put("sakId", sakId.toString())
            put("oppgaveId", oppgaveId.toString())
            put("behandling", behandling)
            put("personIdent", personIdent)
            saksbehandlerIdent?.let { put("saksbehandlerIdent", it) }
            beslutterIdent?.let { put("beslutterIdent", it) }
            put("oppgaveTilstander", oppgaveTilstander)
            put("versjon", versjon)
        }

    constructor(
        oppgave: Oppgave,
        sakId: UUID,
    ) : this(
        behandling =
            StatistikkBehandling(
                behandlingId = oppgave.behandling.behandlingId,
                tidspunkt = oppgave.behandling.opprettet,
                basertPåBehandlingId = null,
                utløstAv =
                    UtløstAv(
                        type = oppgave.behandling.utløstAv.name,
                        tidspunkt = oppgave.behandling.opprettet, // todo
                    ),
            ),
        oppgaveId = oppgave.oppgaveId,
        sakId = sakId,
        personIdent = oppgave.personIdent(),
        saksbehandlerIdent = oppgave.sisteSaksbehandler(),
        beslutterIdent = oppgave.sisteBeslutter(),
        oppgaveTilstander =
            oppgave.tilstandslogg.map {
                StatistikkOppgaveTilstandsendring(
                    tilstand = it.tilstand.name,
                    tidspunkt = it.tidspunkt,
                )
            },
    )

    data class StatistikkBehandling(
        val behandlingId: UUID,
        val tidspunkt: LocalDateTime,
        val basertPåBehandlingId: UUID?,
        val utløstAv: UtløstAv,
    )

    data class UtløstAv(
        val type: String,
        val tidspunkt: LocalDateTime,
    )

    data class StatistikkOppgaveTilstandsendring(
        val tilstand: String,
        val tidspunkt: LocalDateTime,
    )
}
