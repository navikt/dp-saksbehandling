package no.nav.dagpenger.saksbehandling

import java.util.UUID

interface OppgaveRepository {
    fun hentAlleOppgaver(): List<Oppgave>

    fun hent(oppgaveId: UUID): Oppgave?
}
