package no.nav.dagpenger.behandling

import java.time.LocalDateTime

data class Person(val ident: String)

class Behandling private constructor(
    val person: Person,
    val steg: Set<Steg> = emptySet(),
    val opprettet: LocalDateTime,
) {

    constructor(person: Person, steg: Set<Steg>) : this(person, steg, LocalDateTime.now())

    fun nesteSteg(): Set<Steg> {
        return steg.flatMap {
            it.nesteSteg()
        }.toSet()
    }

    fun alleSteg(): Set<Steg> {
        return steg.flatMap {
            it.alleSteg()
        }.toSet()
    }
}

class Svar<T>(val verdi: T) {
    companion object {
        val Ubesvart = Svar(Unit)
    }
}
