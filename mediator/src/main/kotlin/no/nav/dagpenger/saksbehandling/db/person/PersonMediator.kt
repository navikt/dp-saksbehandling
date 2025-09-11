

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.api.Oppslag
import no.nav.dagpenger.saksbehandling.db.person.PersonRepository
import java.util.UUID

class PersonMediator(
    private val personRepository: PersonRepository,
    private val oppslag: Oppslag,
) {
    fun hentPerson(ident: String): Person = personRepository.hentPerson(ident)

    fun hentPerson(id: UUID): Person = personRepository.hentPerson(id)

    fun lagre(person: Person) = personRepository.lagre(person)

    fun finnEllerOpprettPerson(ident: String): Person =
        personRepository.finnPerson(ident) ?: runBlocking {
            coroutineScope {
                val erSkjermet = async { oppslag.erSkjermetPerson(ident) }
                val adressebeskyttelseGradering = async { oppslag.adressebeskyttelseGradering(ident) }
                val person =
                    Person(
                        ident = ident,
                        skjermesSomEgneAnsatte = erSkjermet.await(),
                        adressebeskyttelseGradering = adressebeskyttelseGradering.await(),
                    )
                validerPerson(person)
                personRepository.lagre(person)
                person
            }
        }

    private fun validerPerson(person: Person) {
        if (person.adressebeskyttelseGradering != UGRADERT) {
            throw AdresseBeeskyttetPersonException()
        }
        if (person.skjermesSomEgneAnsatte) {
            throw SkjermetPersonException()
        }
    }
}

internal class AdresseBeeskyttetPersonException : RuntimeException("Person er adressebeskyttet")

internal class SkjermetPersonException : RuntimeException("Person er skjermet")
