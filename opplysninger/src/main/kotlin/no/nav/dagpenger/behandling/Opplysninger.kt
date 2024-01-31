package no.nav.dagpenger.behandling

import java.time.LocalDateTime

interface LesbarOpplysninger {
    fun <T : Comparable<T>> finnOpplysning(
        opplysningstype: Opplysningstype<T>,
        gyldigForDato: LocalDateTime = LocalDateTime.now(),
    ): Opplysning<T>

    fun har(opplysningstype: Opplysningstype<*>): Boolean

    fun trenger(opplysningstype: Opplysningstype<*>): Set<Opplysningstype<*>>

    fun finnAlle(
        opplysningstyper: List<Opplysningstype<*>>,
        fraDato: LocalDateTime = LocalDateTime.now(),
    ): List<Opplysning<*>>
}

class Opplysninger private constructor(
    private val regelmotor: Regelmotor,
    private val opplysninger: MutableMap<Opplysningstype<*>, TemporalCollection<Opplysning<*>>> = mutableMapOf(),
) : LesbarOpplysninger {
    constructor(regelmotor: Regelmotor) : this(regelmotor, mutableMapOf())

    init {
        regelmotor.registrer(this)
    }

    override fun <T : Comparable<T>> finnOpplysning(
        opplysningstype: Opplysningstype<T>,
        gyldigForDato: LocalDateTime,
    ): Opplysning<T> {
        return finnNullableOpplysning(opplysningstype, gyldigForDato)
            ?: throw IllegalStateException("Mangler opplysning $opplysningstype")
    }

    fun leggTil(opplysning: Opplysning<*>) {
        opplysninger.computeIfAbsent(opplysning.opplysningstype) { TemporalCollection() }.put(
            opplysning.gyldighetsperiode.fom,
            opplysning,
        )
        regelmotor.evaluer()
    }

    private fun <T : Comparable<T>> finnNullableOpplysning(
        opplysningstype: Opplysningstype<T>,
        gyldigForDato: LocalDateTime,
    ): Opplysning<T>? = opplysninger[opplysningstype]?.get(gyldigForDato) as Opplysning<T>?

    override fun har(opplysningstype: Opplysningstype<*>) = opplysninger.containsKey(opplysningstype)

    override fun trenger(opplysningstype: Opplysningstype<*>) = regelmotor.trenger(opplysningstype)

    override fun finnAlle(
        opplysningstyper: List<Opplysningstype<*>>,
        fraDato: LocalDateTime,
    ) = opplysningstyper.mapNotNull { finnNullableOpplysning(it, fraDato) }
}
