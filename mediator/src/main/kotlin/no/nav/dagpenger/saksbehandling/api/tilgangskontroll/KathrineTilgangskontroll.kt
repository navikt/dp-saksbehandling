package no.nav.dagpenger.saksbehandling.api.tilgangskontroll

import java.util.UUID

object KathrineTilgangskontroll : OppgaveTilgangskontroll {
    override fun harTilgang(
        oppgaveId: UUID,
        saksbehandler: Saksbehandler,
    ): Boolean = saksbehandler.navIdent.uppercase() == "G151133"

    override fun feilmelding(
        oppgaveId: UUID,
        saksbehandler: Saksbehandler,
    ): String {
        return "Saksbehandler er ikke Kathrine"
    }

    override fun feilType(
        oppgaveId: UUID,
        saksbehandler: Saksbehandler,
    ): String = "KathrineTilgangskontroll"
}
