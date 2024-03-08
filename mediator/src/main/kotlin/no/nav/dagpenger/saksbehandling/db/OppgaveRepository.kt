package no.nav.dagpenger.saksbehandling.db

import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type
import java.util.UUID

interface OppgaveRepository {
    fun hentAlleOppgaver(): List<Oppgave>
    fun hentAlleOppgaverMedTilstand(tilstand: Type): List<Oppgave>
    fun hentOppgave(oppgaveId: UUID): Oppgave?

    fun lagre(oppgave: Oppgave)
}
