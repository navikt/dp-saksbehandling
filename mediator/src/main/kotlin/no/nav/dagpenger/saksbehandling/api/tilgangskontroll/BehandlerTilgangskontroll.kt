package no.nav.dagpenger.saksbehandling.api.tilgangskontroll

import java.util.UUID

class BehandlerTilgangskontroll(
    private val behandlerFunc: (oppgaveId: UUID) -> String?,
) : OppgaveTilgangskontroll {
    override fun harTilgang(
        oppgaveId: UUID,
        saksbehandler: Saksbehandler,
    ): Boolean {
        val eier = behandlerFunc(oppgaveId)
        return eier == null || eier == saksbehandler.navIdent
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
    ): String = "BehandlerTilgangskontroll"
}
