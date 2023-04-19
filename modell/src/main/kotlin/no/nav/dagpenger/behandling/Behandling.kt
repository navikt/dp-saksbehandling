package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.hendelser.SøknadBehandletHendelse
import java.time.LocalDateTime
import java.util.UUID

data class Person(val ident: String) {
    init {
        require(ident.matches(Regex("\\d{11}"))) { "personident må ha 11 siffer" }
    }
}

class Behandling private constructor(
    val person: Person,
    val steg: Set<Steg<*>> = emptySet(),
    val opprettet: LocalDateTime,
    val uuid: UUID = UUID.randomUUID(),
) {
    private var innvilget: Boolean? = null

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

    fun erBehandlet() = innvilget != null

    fun fastsettelser(): Map<String, String> =
        alleSteg().filterIsInstance<Steg.Fastsettelse<*>>().associate { it.id to it.svar.toString() }

    fun håndter(hendelse: SøknadBehandletHendelse) {
        this.innvilget = hendelse.innvilget
    }

    inline fun <reified T> besvar(uuid: UUID, verdi: T) {
        val stegSomSkalBesvares = alleSteg().single { it.uuid == uuid }

        require(stegSomSkalBesvares.svar.clazz == T::class.java) {
            "Fikk ${T::class.java}, forventet ${stegSomSkalBesvares.svar.clazz} ved besvaring av steg med uuid: $uuid"
        }

        (stegSomSkalBesvares as Steg<T>).besvar(verdi)
    }

    fun utfall(): Boolean = steg.filterIsInstance<Steg.Vilkår>().all {
        it.svar.verdi == true
    }

    fun erFerdig(): Boolean =
        steg.filterIsInstance<Steg.Vilkår>().any { it.svar.verdi == false } || steg.none { it.svar.ubesvart }
}

class Svar<T>(val verdi: T?, val clazz: Class<T>) {
    fun besvar(verdi: T) = Svar(verdi, this.clazz)
    fun nullstill() = Svar(null, this.clazz)
    val ubesvart get() = verdi == null

    override fun toString() = verdi.toString()
}
