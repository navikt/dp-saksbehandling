package no.nav.dagpenger.saksbehandling.db

import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Person
import java.util.UUID

internal interface Repository {
    fun lagre(person: Person)
    fun lagre(behandling: Behandling)
    fun hentBehandlingFra(oppgaveId: UUID): Behandling
    fun hentBehandling(behandlingId: UUID): Behandling
    fun hentAlleOppgaver(): List<Oppgave>
    fun hentAlleOppgaverMedTilstand(tilstand: Oppgave.Tilstand.Type): List<Oppgave>
    fun hentOppgave(oppgaveId: UUID): Oppgave?

    fun finnOppgaverFor(ident: String): List<Oppgave>
    fun hentPerson(ident: String): Person?
}
