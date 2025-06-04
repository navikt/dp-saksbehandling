package no.nav.dagpenger.saksbehandling.db.sak

import no.nav.dagpenger.saksbehandling.NySak
import java.util.UUID

interface SakRepository {
    fun lagre(sak: NySak)

    fun hent(sakId: UUID): NySak

    fun finnAlle(): List<NySak>
}

object InMemorySakRepository : SakRepository {
    private val saker: MutableSet<NySak> = mutableSetOf()

    fun reset() {
        saker.clear()
    }

    override fun lagre(sak: NySak) {
        saker.add(sak)
    }

    override fun hent(sakId: UUID): NySak {
        return saker.single { it.id == sakId }
    }

    override fun finnAlle(): List<NySak> {
        return saker.toList()
    }
}
