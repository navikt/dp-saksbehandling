package no.nav.dagpenger.saksbehandling.db

import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.Person
import java.util.UUID

interface PersonRepository {
    fun lagre(person: Person)
    fun lagre(behandling: Behandling)

    fun hentBehandlingFra(oppgaveId: UUID): Behandling
    fun hentBehandling(behandlingId: UUID): Behandling
    fun hentPerson(ident: String): Person?
}
