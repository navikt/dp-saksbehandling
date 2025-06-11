package no.nav.dagpenger.saksbehandling.db.person

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.api.Oppslag
import java.util.UUID

class PersonMediator(
    private val personRepository: PersonRepository,
    private val oppslag: Oppslag,
) {
    fun finnPerson(ident: String): Person? = personRepository.finnPerson(ident)

    fun finnPerson(id: UUID): Person? = personRepository.finnPerson(id)

    fun hentPerson(ident: String): Person = personRepository.hentPerson(ident)

    fun hentPerson(id: UUID): Person = personRepository.hentPerson(id)

    fun lagre(person: Person) = personRepository.lagre(person)

    fun finnEllerOpprettPerson(ident: String): Person {
        return personRepository.finnPerson(ident) ?: runBlocking {
            coroutineScope {
                val erSkjermet = async { oppslag.erSkjermetPerson(ident) }
                val erAdresseBeskyttet = async { oppslag.erAdressebeskyttetPerson(ident) }
                val person =
                    Person(
                        ident = ident,
                        skjermesSomEgneAnsatte = erSkjermet.await(),
                        adressebeskyttelseGradering = erAdresseBeskyttet.await(),
                    )
                personRepository.lagre(person)
                person
            }
        }
    }
}
