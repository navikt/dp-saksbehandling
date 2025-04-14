package no.nav.dagpenger.saksbehandling.db.person

import no.nav.dagpenger.saksbehandling.Person

interface PersonRepository {
    fun finnPerson(ident: String): Person?

    fun hentPerson(ident: String): Person

    fun lagre(person: Person)
}
