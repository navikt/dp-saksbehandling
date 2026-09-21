package no.nav.dagpenger.saksbehandling.db.person

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.api.Oppslag
import no.nav.dagpenger.saksbehandling.db.Transaksjonskontekst
import no.nav.dagpenger.saksbehandling.db.Transaksjonskontekst.IkkeAktiv
import java.util.UUID

class PersonMediator(
    private val personRepository: PersonRepository,
    private val oppslag: Oppslag,
) {
    fun hentPersonForBehandlingId(
        behandlingId: UUID,
        saksbehandler: Saksbehandler,
    ): Person =
        personRepository.hentPersonForBehandlingId(behandlingId).also {
            it.harTilgang(saksbehandler)
        }

    fun hentPerson(
        ident: String,
        saksbehandler: Saksbehandler,
    ): Person =
        personRepository.hentPerson(ident).also {
            it.harTilgang(saksbehandler)
        }

    fun hentPerson(
        personId: UUID,
        saksbehandler: Saksbehandler,
    ): Person =
        personRepository.hentPerson(personId = personId).also {
            it.harTilgang(saksbehandler)
        }

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
                        inhabileNavIdenter = emptyList(),
                    )
                validerPerson(person)
                personRepository.lagre(person)
                person
            }
        }

    fun erNødbremset(ident: String) = personRepository.erNødbremset(ident)

    /**
     * Registrerer at [navIdent] er inhabil for [person]. Inhabilitet er permanent og gjelder personen,
     * ikke enkeltoppgaver.
     */
    fun registrerInhabilitet(
        person: Person,
        navIdent: String,
        ctx: Transaksjonskontekst = IkkeAktiv,
    ) {
        val oppdatertPerson = person.registrerInhabilitet(navIdent)
        personRepository.lagre(oppdatertPerson, ctx)
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
