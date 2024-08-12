package no.nav.dagpenger.saksbehandling.api.tilgangskontroll

import no.nav.dagpenger.saksbehandling.AdresseBeskyttelseGradering
import no.nav.dagpenger.saksbehandling.AdresseBeskyttelseGradering.FORTROLIG
import no.nav.dagpenger.saksbehandling.AdresseBeskyttelseGradering.STRENGT_FORTROLIG
import no.nav.dagpenger.saksbehandling.AdresseBeskyttelseGradering.STRENGT_FORTROLIG_UTLAND
import no.nav.dagpenger.saksbehandling.AdresseBeskyttelseGradering.UGRADERT
import java.util.UUID

class AdressebeskyttelseTilgangskontroll(
    private val strengtFortroligGruppe: String,
    private val fortroligGruppe: String,
    private val adressebeskyttelseGraderingFun: (oppgaveId: UUID) -> AdresseBeskyttelseGradering,
) : OppgaveTilgangskontroll {
    override fun harTilgang(
        oppgaveId: UUID,
        saksbehandler: Saksbehandler,
    ): Boolean {
        val gradering = adressebeskyttelseGraderingFun(oppgaveId)
        return when (gradering) {
            UGRADERT -> true
            STRENGT_FORTROLIG_UTLAND -> saksbehandler.grupper.contains(strengtFortroligGruppe)
            STRENGT_FORTROLIG -> saksbehandler.grupper.contains(strengtFortroligGruppe)
            FORTROLIG -> saksbehandler.grupper.contains(fortroligGruppe)
        }
    }

    override fun feilmelding(
        oppgaveId: UUID,
        saksbehandler: Saksbehandler,
    ): String {
        return "${saksbehandler.navIdent} har ikke tilgang til oppgave $oppgaveId"
    }
}
