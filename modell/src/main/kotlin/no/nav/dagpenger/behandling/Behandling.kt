package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.hendelser.SøknadBehandletHendelse
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Person(val ident: String) {
    init {
        require(ident.matches(Regex("\\d{11}"))) { "personident må ha 11 siffer, fikk ${ident.length}" }
    }
}

interface Behandlingsstatus {
    fun utfall(): Boolean
    fun erFerdig(): Boolean
}

interface Svarbart {
    fun besvar(uuid: UUID, verdi: String)
    fun besvar(uuid: UUID, verdi: Int)
    fun besvar(uuid: UUID, verdi: LocalDate)
    fun besvar(uuid: UUID, verdi: Boolean)
}

class Behandling private constructor(
    val person: Person,
    val steg: Set<Steg<*>> = emptySet(),
    val opprettet: LocalDateTime,
    val uuid: UUID = UUID.randomUUID(),
) : Behandlingsstatus, Svarbart {
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

    override fun utfall(): Boolean = steg.filterIsInstance<Steg.Vilkår>().all {
        it.svar.verdi == true
    }

    override fun erFerdig(): Boolean =
        steg.filterIsInstance<Steg.Vilkår>().any { it.svar.verdi == false } || steg.none { it.svar.ubesvart }

    override fun besvar(uuid: UUID, verdi: String) = _besvar(uuid, verdi)

    override fun besvar(uuid: UUID, verdi: Int) = _besvar(uuid, verdi)

    override fun besvar(uuid: UUID, verdi: LocalDate) = _besvar(uuid, verdi)

    override fun besvar(uuid: UUID, verdi: Boolean) = _besvar(uuid, verdi)

    private inline fun <reified T> _besvar(uuid: UUID, verdi: T) {
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

    override fun toString() = verdi.toString()
}
