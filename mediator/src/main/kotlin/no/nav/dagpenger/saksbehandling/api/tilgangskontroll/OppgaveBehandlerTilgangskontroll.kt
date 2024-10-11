package no.nav.dagpenger.saksbehandling.api.tilgangskontroll

import no.nav.dagpenger.saksbehandling.Saksbehandler
import java.util.UUID

class OppgaveBehandlerTilgangskontroll(
    private val behandlerFunc: (oppgaveId: UUID) -> String?,
) : OppgaveTilgangskontroll {
    override fun harTilgang(
        oppgaveId: UUID,
        saksbehandler: Saksbehandler,
    ): Boolean {
        val eier = behandlerFunc(oppgaveId)
        return eier == saksbehandler.navIdent
    }

    override fun feilmelding(
        oppgaveId: UUID,
        saksbehandler: Saksbehandler,
    ): String {
        return "Saksbehandler ${saksbehandler.navIdent} har ikke tilgang til oppgave med id $oppgaveId"
    }

    override fun feilType(
        oppgaveId: UUID,
        saksbehandler: Saksbehandler,
    ): String = "OppgaveBehandlerTilgangskontroll"
}
