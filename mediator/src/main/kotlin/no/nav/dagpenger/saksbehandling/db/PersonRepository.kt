package no.nav.dagpenger.saksbehandling.db

import no.nav.dagpenger.saksbehandling.Person

interface PersonRepository {
    fun lagre(person: Person)

    fun hent(ident: String): Person?
}
