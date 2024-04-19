package no.nav.dagpenger.saksbehandling.db

import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Person
import java.util.UUID

internal interface Repository {
    fun finnPerson(ident: String): Person?
    fun hentPerson(ident: String): Person
    fun lagre(person: Person)

    fun finnBehandling(behandlingId: UUID): Behandling?
    fun hentBehandling(behandlingId: UUID): Behandling
    fun hentBehandlingFra(oppgaveId: UUID): Behandling
    fun lagre(behandling: Behandling)
    fun slettBehandling(behandlingId: UUID)

    fun hentOppgave(oppgaveId: UUID): Oppgave
    fun lagre(oppgave: Oppgave)

    fun finnOppgaverFor(ident: String): List<Oppgave>
    fun finnSaksbehandlersOppgaver(saksbehandlerIdent: String): List<Oppgave>
    fun hentAlleOppgaverMedTilstand(tilstand: Oppgave.Tilstand.Type): List<Oppgave>
    fun hentNesteOppgavenTil(saksbehandlerIdent: String): Oppgave?
}
