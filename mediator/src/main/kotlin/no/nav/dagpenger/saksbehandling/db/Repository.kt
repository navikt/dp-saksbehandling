package no.nav.dagpenger.saksbehandling.db

import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Person
import java.util.UUID

internal interface Repository {
    fun lagre(person: Person)
    fun lagre(behandling: Behandling)
    fun lagre(oppgave: Oppgave)
    fun hentBehandlingFra(oppgaveId: UUID): Behandling
    fun hentBehandling(behandlingId: UUID): Behandling
    fun finnBehandling(behandlingId: UUID): Behandling?
    fun hentAlleOppgaverMedTilstand(tilstand: Oppgave.Tilstand.Type): List<Oppgave>
    fun hentOppgave(oppgaveId: UUID): Oppgave?
    fun finnOppgaverFor(ident: String): List<Oppgave>
    fun finnPerson(ident: String): Person?
    fun hentPerson(ident: String): Person
}
