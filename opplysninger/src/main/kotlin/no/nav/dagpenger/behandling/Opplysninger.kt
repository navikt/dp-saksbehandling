package no.nav.dagpenger.behandling

interface LesbarOpplysninger {
    fun <T : Comparable<T>> finnOpplysning(opplysningstype: Opplysningstype<T>): Opplysning<T>

    fun har(opplysningstype: Opplysningstype<*>): Boolean

    fun trenger(opplysningstype: Opplysningstype<*>): Set<Opplysningstype<*>>

    fun finnAlle(opplysningstyper: List<Opplysningstype<*>>): List<Opplysning<*>>
}

class Opplysninger(
    private val regelmotor: Regelmotor,
    private val opplysninger: MutableList<Opplysning<*>> = mutableListOf(),
) : LesbarOpplysninger {
    init {
        regelmotor.registrer(this)
    }

    override fun <T : Comparable<T>> finnOpplysning(opplysningstype: Opplysningstype<T>): Opplysning<T> =
        opplysninger.find { it.er(opplysningstype) } as Opplysning<T>

    fun leggTil(opplysning: Opplysning<*>) {
        opplysninger.add(opplysning)
        regelmotor.evaluer()
    }

    override fun har(opplysningstype: Opplysningstype<*>) = opplysninger.any { it.er(opplysningstype) }

    override fun trenger(opplysningstype: Opplysningstype<*>) = regelmotor.trenger(opplysningstype)

    override fun finnAlle(opplysningstyper: List<Opplysningstype<*>>) =
        opplysningstyper.flatMap { opplysninger.filter { opplysning -> opplysning.er(it) } }
}
