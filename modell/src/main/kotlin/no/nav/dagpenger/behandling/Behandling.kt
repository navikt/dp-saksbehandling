package no.nav.dagpenger.behandling

import java.time.LocalDateTime
import java.util.UUID

data class Person(val ident: String)

class Behandling private constructor(
    val person: Person,
    val steg: Set<Steg<*>> = emptySet(),
    val opprettet: LocalDateTime,
    val uuid: UUID = UUID.randomUUID(),
) {
    constructor(person: Person, steg: Set<Steg<*>>) : this(person, steg, LocalDateTime.now())

    fun nesteSteg(): Set<Steg<*>> {
        val map = steg.flatMap {
            it.nesteSteg()
        }
        return map.toSet()
    }

    fun alleSteg(): Set<Steg<*>> {
        return steg.flatMap {
            it.alleSteg()
        }.toSet()
    }

    inline fun <reified T> besvar(uuid: UUID, verdi: T) {
        val stegSomSkalBesvares = alleSteg().single { it.uuid == uuid }

        require(stegSomSkalBesvares.svar.clazz == T::class.java) {
            "Fikk ${T::class.java}, forventet ${stegSomSkalBesvares.svar.clazz} ved besvaring av steg med uuid: $uuid"
        }

        (stegSomSkalBesvares as Steg<T>).besvar(verdi)
    }
}

class Svar<T>(val verdi: T?, val clazz: Class<T>) {
    fun besvar(verdi: T) = Svar(verdi, this.clazz)
    fun nullstill() = Svar(null, this.clazz)
    val ubesvart get() = verdi == null
}
