package no.nav.dagpenger.saksbehandling.db.person

import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.db.Transaksjonskontekst
import no.nav.dagpenger.saksbehandling.db.Transaksjonskontekst.IkkeAktiv
import java.util.UUID

interface PersonRepository {
    fun finnPerson(ident: String): Person?

    fun finnPerson(personId: UUID): Person?

    fun hentPerson(ident: String): Person

    fun hentPerson(personId: UUID): Person

    fun hentPersonForBehandlingId(behandlingId: UUID): Person

    fun lagre(
        person: Person,
        ctx: Transaksjonskontekst = IkkeAktiv,
    )

    fun erNødbremset(ident: String): Boolean
}
