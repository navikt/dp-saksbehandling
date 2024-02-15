package no.nav.dagpenger.saksbehandling

interface PersonRepository {
    fun lagre(person: Person)

    fun hent(ident: String): Person?
}

class InMemoryPersonRepository : PersonRepository {
    private val personMap = mutableMapOf<String, Person>()

    override fun lagre(person: Person) {
        personMap[person.ident] = person
    }

    override fun hent(ident: String): Person? = personMap[ident]

    fun slettAlt() = personMap.clear()
}
