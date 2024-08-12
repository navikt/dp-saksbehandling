package no.nav.dagpenger.saksbehandling.api.tilgangskontroll

import java.util.UUID

class EgneAnsatteTilgangskontroll(
    private val tillatteGrupper: Set<String>,
    private val skjermesSomEgneAnsatteFun: (UUID) -> Boolean?,
) : OppgaveTilgangskontroll {
    override fun harTilgang(
        oppgaveId: UUID,
        saksbehandler: Saksbehandler,
    ): Boolean {
        return when (skjermesSomEgneAnsatteFun(oppgaveId)) {
            true -> saksbehandler.grupper.any { it in tillatteGrupper }
            else -> true
        }
    }

    override fun feilmelding(
        oppgaveId: UUID,
        saksbehandler: Saksbehandler,
    ): String {
        return "${saksbehandler.navIdent} har ikke tilgang til oppgave $oppgaveId"
    }
}
