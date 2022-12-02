package no.nav.dagpenger.behandling.db

import no.nav.dagpenger.behandling.Person

interface PersonRepository {
    fun hentPerson(ident: String) : Person?
    fun lagrePerson(person: Person)
}

