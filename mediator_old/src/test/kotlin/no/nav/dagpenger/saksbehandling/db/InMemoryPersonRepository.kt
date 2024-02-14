package no.nav.dagpenger.saksbehandling.db

import no.nav.dagpenger.saksbehandling.Person

object InMemoryPersonRepository : PersonRepository {
    private val personer = mutableSetOf<Person>()

    override fun hentPerson(ident: String): Person? {
        return personer.firstOrNull { it.ident == ident }
    }

    override fun lagrePerson(person: Person) {
        personer.add(person)
    }
}
