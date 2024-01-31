package no.nav.dagpenger.behandling

import java.time.LocalDate
import java.time.LocalDateTime

interface LesbarOpplysninger {
    fun <T : Comparable<T>> finnOpplysning(
        opplysningstype: Opplysningstype<T>,
        gyldigForDato: LocalDateTime = LocalDateTime.now(),
    ): Opplysning<T>

    fun <T : Comparable<T>> finnOpplysning(
        opplysningstype: Opplysningstype<T>,
        gyldigForDato: LocalDate,
    ): Opplysning<T>

    fun har(
        opplysningstype: Opplysningstype<*>,
        fraDato: LocalDateTime,
    ): Boolean

    fun har(
        opplysningstype: Opplysningstype<*>,
        fraDato: LocalDate,
    ): Boolean

    fun trenger(
        opplysningstype: Opplysningstype<*>,
        fraDato: LocalDateTime,
    ): Set<Opplysningstype<*>>

    fun trenger(
        opplysningstype: Opplysningstype<*>,
        fraDato: LocalDate,
    ): Set<Opplysningstype<*>>

    fun finnAlle(
        opplysningstyper: List<Opplysningstype<*>>,
        fraDato: LocalDateTime = LocalDateTime.now(),
    ): List<Opplysning<*>>

    fun finnAlle(
        opplysningstyper: List<Opplysningstype<*>>,
        fraDato: LocalDate,
    ): List<Opplysning<*>>
}

class Opplysninger private constructor(
    private val regelmotor: Regelmotor,
    private val opplysninger: MutableList<Opplysning<*>>,
) : LesbarOpplysninger {
    constructor(regelmotor: Regelmotor) : this(regelmotor, mutableListOf())

    init {
        regelmotor.registrer(this)
    }

    override fun <T : Comparable<T>> finnOpplysning(
        opplysningstype: Opplysningstype<T>,
        gyldigForDato: LocalDateTime,
    ): Opplysning<T> {
        return finnNullableOpplysning(opplysningstype, gyldigForDato)
            ?: throw IllegalStateException("Har ikke opplysning $opplysningstype som er gyldig for $gyldigForDato")
    }

    override fun <T : Comparable<T>> finnOpplysning(
        opplysningstype: Opplysningstype<T>,
        gyldigForDato: LocalDate,
    ): Opplysning<T> = finnOpplysning(opplysningstype, gyldigForDato.atStartOfDay())

    fun leggTil(opplysning: Opplysning<*>) {
        require(
            opplysninger.none {
                it.sammeSom(opplysning)
            },
        ) { "Opplysning ${opplysning.opplysningstype} finnes allerede med overlappende gyldighetsperiode" }
        opplysninger.add(opplysning)
        regelmotor.evaluer()
    }

    private fun <T : Comparable<T>> finnNullableOpplysning(
        opplysningstype: Opplysningstype<T>,
        gyldigForDato: LocalDateTime,
    ): Opplysning<T>? =
        opplysninger.firstOrNull { it.er(opplysningstype) && it.gyldighetsperiode.inneholder(gyldigForDato) } as Opplysning<T>?

    override fun har(
        opplysningstype: Opplysningstype<*>,
        fraDato: LocalDateTime,
    ) = finnNullableOpplysning(opplysningstype, fraDato) != null

    override fun har(
        opplysningstype: Opplysningstype<*>,
        fraDato: LocalDate,
    ) = har(opplysningstype, fraDato.atStartOfDay())

    override fun trenger(
        opplysningstype: Opplysningstype<*>,
        fraDato: LocalDateTime,
    ) = regelmotor.trenger(opplysningstype, fraDato)

    override fun trenger(
        opplysningstype: Opplysningstype<*>,
        fraDato: LocalDate,
    ) = trenger(opplysningstype, fraDato.atStartOfDay())

    override fun finnAlle(
        opplysningstyper: List<Opplysningstype<*>>,
        fraDato: LocalDateTime,
    ) = opplysningstyper.mapNotNull { finnNullableOpplysning(it, fraDato) }

    override fun finnAlle(
        opplysningstyper: List<Opplysningstype<*>>,
        fraDato: LocalDate,
    ) = finnAlle(opplysningstyper, fraDato.atStartOfDay())
}
