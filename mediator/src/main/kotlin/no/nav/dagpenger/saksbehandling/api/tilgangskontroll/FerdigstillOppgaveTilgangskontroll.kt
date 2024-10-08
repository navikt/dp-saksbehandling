package no.nav.dagpenger.saksbehandling.api.tilgangskontroll

import no.nav.dagpenger.saksbehandling.Oppgave
import java.util.UUID

class FerdigstillOppgaveTilgangskontroll(
    private val oppgaveFunc: (oppgaveId: UUID) -> Oppgave,
) : OppgaveTilgangskontroll {
    override fun harTilgang(
        oppgaveId: UUID,
        saksbehandler: Saksbehandler,
    ): Boolean {
        val oppgave = oppgaveFunc(oppgaveId)

        val sammeEier =
            OppgaveBehandlerTilgangskontroll({ oppgave.behandlerIdent }).harTilgang(oppgaveId, saksbehandler)

        return when (oppgave.tilstand().type) {
            Oppgave.Tilstand.Type.UNDER_BEHANDLING -> sammeEier
            Oppgave.Tilstand.Type.UNDER_KONTROLL ->
                sammeEier &&
                    BeslutterRolleTilgangskontroll.harTilgang(
                        oppgaveId,
                        saksbehandler,
                    )
            else -> false
        }
    }

    override fun feilmelding(
        oppgaveId: UUID,
        saksbehandler: Saksbehandler,
    ): String {
        return "#${saksbehandler.navIdent} har ikke tilgang til å ferdigstille oppgave med id $oppgaveId"
    }

    override fun feilType(
        oppgaveId: UUID,
        saksbehandler: Saksbehandler,
    ): String {
        return "FerdigstillOppgaveTilgangskontroll"
    }
}
