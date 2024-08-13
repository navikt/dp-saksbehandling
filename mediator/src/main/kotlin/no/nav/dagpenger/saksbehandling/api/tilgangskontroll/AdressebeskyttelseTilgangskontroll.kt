package no.nav.dagpenger.saksbehandling.api.tilgangskontroll

import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.FORTROLIG
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.STRENGT_FORTROLIG
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import java.util.UUID

class AdressebeskyttelseTilgangskontroll(
    private val strengtFortroligGruppe: String,
    private val strengtFortroligUtlandGruppe: String,
    private val fortroligGruppe: String,
    private val adressebeskyttelseGraderingFun: (oppgaveId: UUID) -> AdressebeskyttelseGradering,
) : OppgaveTilgangskontroll {
    override fun harTilgang(
        oppgaveId: UUID,
        saksbehandler: Saksbehandler,
    ): Boolean {
        val gradering = adressebeskyttelseGraderingFun(oppgaveId)
        return when (gradering) {
            UGRADERT -> true
            STRENGT_FORTROLIG_UTLAND -> saksbehandler.grupper.contains(strengtFortroligUtlandGruppe)
            STRENGT_FORTROLIG -> saksbehandler.grupper.contains(strengtFortroligGruppe)
            FORTROLIG -> saksbehandler.grupper.contains(fortroligGruppe)
        }
    }

    override fun feilmelding(
        oppgaveId: UUID,
        saksbehandler: Saksbehandler,
    ): String {
        return "${saksbehandler.navIdent} har ikke tilgang til adressebeskyttet oppgave. OppgaveId: $oppgaveId"
    }
}
