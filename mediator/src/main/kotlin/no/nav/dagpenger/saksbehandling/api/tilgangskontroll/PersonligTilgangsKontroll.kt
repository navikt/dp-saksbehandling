package no.nav.dagpenger.saksbehandling.api.tilgangskontroll

import no.nav.dagpenger.saksbehandling.Saksbehandler
import java.util.UUID

object PersonligTilgangsKontroll : OppgaveTilgangskontroll {
    override fun harTilgang(
        oppgaveId: UUID,
        saksbehandler: Saksbehandler,
    ): Boolean = saksbehandler.navIdent.uppercase() in listOf("G151133", "Z994251", "Z993298", "Z994854")

    override fun feilmelding(
        oppgaveId: UUID,
        saksbehandler: Saksbehandler,
    ): String {
        return "Saksbehandler ${saksbehandler.navIdent} har ikke tilgang til oppgave med id $oppgaveId"
    }

    override fun feilType(
        oppgaveId: UUID,
        saksbehandler: Saksbehandler,
    ): String = "PersonligTilgangsKontroll"
}
