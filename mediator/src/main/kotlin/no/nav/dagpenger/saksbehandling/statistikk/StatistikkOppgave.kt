package no.nav.dagpenger.saksbehandling.statistikk

import no.nav.dagpenger.saksbehandling.Oppgave
import java.time.LocalDateTime
import java.util.UUID

data class StatistikkOppgave constructor(
    val behandling: StatistikkBehandling,
    val sakId: UUID,
    val personIdent: String,
    val saksbehandlerIdent: String?,
    val beslutterIdent: String?,
    val oppgaveTilstander: List<StatistikkOppgaveTilstandsendring>,
) {
    fun asMap(): Map<String, Any> =
        buildMap {
            put("sakId", sakId.toString())
            put("behandling", behandling)
            put("personIdent", personIdent)
            saksbehandlerIdent?.let { put("saksbehandlerIdent", it) }
            beslutterIdent?.let { put("beslutterIdent", it) }
            put("oppgaveTilstander", oppgaveTilstander)
        }

    constructor(
        oppgave: Oppgave,
        sakId: UUID,
    ) : this(
        behandling =
            StatistikkBehandling(
                id = oppgave.behandling.behandlingId,
                tidspunkt = oppgave.behandling.opprettet,
                basertPåBehandling = null,
                utløstAv =
                    UtløstAv(
                        type = oppgave.behandling.utløstAv.name,
                        tidspunkt = oppgave.behandling.opprettet, // todo
                    ),
            ),
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
        val id: UUID,
        val tidspunkt: LocalDateTime,
        val basertPåBehandling: UUID?,
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
