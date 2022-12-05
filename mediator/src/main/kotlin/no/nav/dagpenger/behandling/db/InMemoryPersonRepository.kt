package no.nav.dagpenger.behandling.db

import no.nav.dagpenger.behandling.Person

class InMemoryPersonRepository : PersonRepository {

    private val personer = mutableMapOf<String, Person>()
    override fun hentPerson(ident: String): Person? = personer[ident]

    override fun lagrePerson(person: Person) {
        personer[person.ident()] = person
    }

}