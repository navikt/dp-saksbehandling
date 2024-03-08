package no.nav.dagpenger.saksbehandling.db

import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.Person
import java.util.UUID

interface PersonRepository {
    fun lagre(person: Person)
    fun lagre(behandling: Behandling)

    fun hentBehandling(oppgaveId: UUID): Behandling
    fun hentBehandlingFra(behandlingId: UUID): Behandling
    fun hentBehandlingFra(ident: String): Person?
}
