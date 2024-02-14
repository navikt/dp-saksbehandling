package no.nav.dagpenger.saksbehandling.db

import no.nav.dagpenger.saksbehandling.oppgave.Oppgave
import java.util.UUID

interface OppgaveRepository {
    fun lagreOppgave(oppgave: Oppgave)

    fun hentOppgave(uuid: UUID): Oppgave

    fun hentOppgaver(): List<Oppgave>

    fun hentOppgaveFor(søknadUUID: UUID): Oppgave

    fun hentOppgaverFor(fnr: String): List<Oppgave>
}
