package no.nav.dagpenger.saksbehandling.api.tilgangskontroll

import no.nav.dagpenger.saksbehandling.Saksbehandler
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
            true -> harTilgang(saksbehandler)
            else -> true
        }
    }

    fun harTilgang(saksbehandler: Saksbehandler): Boolean {
        return saksbehandler.grupper.any { it in tillatteGrupper }
    }

    override fun feilmelding(
        oppgaveId: UUID,
        saksbehandler: Saksbehandler,
    ): String {
        return "${saksbehandler.navIdent} har ikke tilgang til oppgave for egne ansatte. OppgaveId: $oppgaveId"
    }

    override fun feilType(
        oppgaveId: UUID,
        saksbehandler: Saksbehandler,
    ): String = "egne-ansatte"
}
