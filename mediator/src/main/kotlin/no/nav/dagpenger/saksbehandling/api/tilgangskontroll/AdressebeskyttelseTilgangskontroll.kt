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
    ): Boolean = tilganger(saksbehandler).contains(adressebeskyttelseGraderingFun(oppgaveId))

    fun tilganger(saksbehandler: Saksbehandler): Set<AdressebeskyttelseGradering> {
        return (
            saksbehandler.grupper.map {
                when (it) {
                    strengtFortroligGruppe -> STRENGT_FORTROLIG
                    strengtFortroligUtlandGruppe -> STRENGT_FORTROLIG_UTLAND
                    fortroligGruppe -> FORTROLIG
                    else -> UGRADERT
                }
            } + UGRADERT
        ).toSet()
    }

    override fun feilmelding(
        oppgaveId: UUID,
        saksbehandler: Saksbehandler,
    ): String {
        return "${saksbehandler.navIdent} har ikke tilgang til adressebeskyttet oppgave. OppgaveId: $oppgaveId"
    }

    override fun feilType(
        oppgaveId: UUID,
        saksbehandler: Saksbehandler,
    ): String = adressebeskyttelseGraderingFun(oppgaveId).name.lowercase().replace("_", "-")
}
