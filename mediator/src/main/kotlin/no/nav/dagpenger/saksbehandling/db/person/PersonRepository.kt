package no.nav.dagpenger.saksbehandling.db.person

import no.nav.dagpenger.saksbehandling.Person
import java.util.UUID

interface PersonRepository {
    fun finnPerson(ident: String): Person?

    fun finnPerson(id: UUID): Person?

    fun hentPerson(ident: String): Person

    fun hentPerson(id: UUID): Person

    fun lagre(person: Person)
}
