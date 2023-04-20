package no.nav.dagpenger.behandling.oppgave

import java.util.UUID

interface OppgaveRepository {
    fun lagreOppgave(oppgave: Oppgave)
    fun hentOppgave(uuid: UUID): Oppgave
    fun hentOppgaver(): List<Oppgave>
    fun hentOppgaverFor(fnr: String): List<Oppgave>
}
