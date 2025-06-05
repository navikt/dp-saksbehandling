package no.nav.dagpenger.saksbehandling.db.sak

import no.nav.dagpenger.saksbehandling.NyPerson
import no.nav.dagpenger.saksbehandling.NySak
import java.util.UUID

interface SakRepository {
    fun lagre(sak: NySak)

    fun hent(sakId: UUID): NySak

    fun finnAlle(): Set<NySak>
}

interface NyPersonRepository {
    fun lagre(person: NyPerson)

    fun hent(ident: String): NyPerson

    fun finn(ident: String): NyPerson?
}

object InmemoryRepository : SakRepository, NyPersonRepository {
    private val saker: MutableSet<NySak> = mutableSetOf()
    private val personer: MutableSet<NyPerson> = mutableSetOf()

    fun reset() {
        saker.clear()
    }

    override fun lagre(sak: NySak) {
        saker.add(sak)
    }

    override fun hent(sakId: UUID): NySak {
        return saker.single { it.id == sakId }
    }

    override fun lagre(person: NyPerson) {
        personer.add(person)
    }

    override fun hent(ident: String): NyPerson {
        return personer.single { it.ident == ident }
    }

    override fun finn(ident: String): NyPerson? {
        return personer.find { it.ident == ident }
    }

    override fun finnAlle(): Set<NySak> {
        return saker
    }
}
