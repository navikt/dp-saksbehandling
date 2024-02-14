package no.nav.dagpenger.saksbehandling.db

import no.nav.dagpenger.saksbehandling.Person

interface PersonRepository {
    fun hentPerson(ident: String): Person?

    fun lagrePerson(person: Person)
}
