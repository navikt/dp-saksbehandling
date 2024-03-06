package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type
import java.util.UUID

interface OppgaveRepository {
    fun hentAlleOppgaver(): List<Oppgave>
    fun hentAlleOppgaverMedTilstand(tilstand: Type): List<Oppgave>
    fun hent(oppgaveId: UUID): Oppgave?
}
