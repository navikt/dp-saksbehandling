package no.nav.dagpenger.saksbehandling.api.tilgangskontroll

import no.nav.dagpenger.saksbehandling.Configuration
import java.util.UUID

object BeslutterRolleTilgangskontroll :
    OppgaveTilgangskontroll {
    override fun harTilgang(
        oppgaveId: UUID,
        saksbehandler: Saksbehandler,
    ): Boolean {
        return saksbehandler.grupper.contains(Configuration.beslutterADGruppe)
    }

    override fun feilmelding(
        oppgaveId: UUID,
        saksbehandler: Saksbehandler,
    ): String {
        return "Saksbehandler ${saksbehandler.navIdent} er ikke beslutter og" +
            "har ikke tilgang til oppgave med id $oppgaveId"
    }

    override fun feilType(
        oppgaveId: UUID,
        saksbehandler: Saksbehandler,
    ): String = "BeslutterRolleTilgangskontroll"
}
