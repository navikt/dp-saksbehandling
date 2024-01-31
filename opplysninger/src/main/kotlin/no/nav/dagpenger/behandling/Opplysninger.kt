package no.nav.dagpenger.behandling

interface LesbarOpplysninger {
    fun <T : Comparable<T>> finnOpplysning(opplysningstype: Opplysningstype<T>): Opplysning<T>

    fun har(opplysningstype: Opplysningstype<*>): Boolean

    fun trenger(opplysningstype: Opplysningstype<*>): Set<Opplysningstype<*>>

    fun finnAlle(opplysningstyper: List<Opplysningstype<*>>): List<Opplysning<*>>
}

class Opplysninger(
    val regelkjøring: Regelkjøring,
    opplysninger: List<Opplysning<*>> = emptyList(),
) : LesbarOpplysninger {
    private val opplysninger: MutableList<Opplysning<*>> = opplysninger.toMutableList()

    constructor(regelkjøring: Regelkjøring) : this(regelkjøring, mutableListOf())

    init {
        regelkjøring.registrer(this)
    }

    override fun <T : Comparable<T>> finnOpplysning(opplysningstype: Opplysningstype<T>): Opplysning<T> {
        return finnNullableOpplysning(opplysningstype)
            ?: throw IllegalStateException("Har ikke opplysning $opplysningstype som er gyldig for ${regelkjøring.forDato}")
    }

    fun leggTil(opplysning: Opplysning<*>) {
        require(opplysninger.none { it.sammeSom(opplysning) }) {
            "Opplysning ${opplysning.opplysningstype} finnes allerede med overlappende gyldighetsperiode"
        }
        opplysninger.add(opplysning)
        regelkjøring.evaluer()
    }

    private fun <T : Comparable<T>> finnNullableOpplysning(opplysningstype: Opplysningstype<T>): Opplysning<T>? =
        opplysninger.firstOrNull { it.er(opplysningstype) && it.gyldighetsperiode.inneholder(regelkjøring.forDato) } as Opplysning<T>?

    override fun har(opplysningstype: Opplysningstype<*>) = finnNullableOpplysning(opplysningstype) != null

    override fun trenger(opplysningstype: Opplysningstype<*>) = regelkjøring.trenger(opplysningstype, regelkjøring.forDato)

    override fun finnAlle(opplysningstyper: List<Opplysningstype<*>>) = opplysningstyper.mapNotNull { finnNullableOpplysning(it) }
}
