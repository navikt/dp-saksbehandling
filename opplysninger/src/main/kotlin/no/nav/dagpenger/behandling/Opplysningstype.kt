package no.nav.dagpenger.behandling

interface Klassifiserbart {
    fun er(type: Opplysningstype<*>): Boolean
}

class Opplysningstype<T : Comparable<T>>(
    private val navn: String,
    private val parent: Opplysningstype<T>? = null,
    private val child: MutableSet<Opplysningstype<*>> = mutableSetOf(),
    val utledesAv: MutableSet<Opplysningstype<*>> = mutableSetOf(),
    val regel: Regel<T> = NullRegel(),
) : Klassifiserbart {
    init {
        parent?.child?.add(this)
        utledesAv.addAll(regel.avhengerAv)
    }

    fun bestårAv(): Set<Opplysningstype<*>> =
        (
            utledesAv.toSet() +
                utledesAv.map {
                    it.bestårAv()
                }.flatten()
        ).filter { it.utledesAv.isEmpty() }.toSet()

    override fun er(type: Opplysningstype<*>): Boolean {
        return navn == type.navn || parent?.er(type) ?: false
    }

    override fun toString(): String {
        // TODO: Det er noe muffens med hierarki og data class
        return navn
    }

    override fun equals(other: Any?): Boolean = other is Opplysningstype<*> && other.navn == this.navn && other.parent == this.parent

    override fun hashCode(): Int {
        var result = navn.hashCode()
        result = 31 * result + (parent?.hashCode() ?: 0)
        return result
    }
}

abstract class Regel<T : Comparable<T>>(
    val avhengerAv: List<Opplysningstype<*>> = emptyList(),
) {
    abstract fun kjør(opplysninger: List<Opplysning<*>>)
}

class NullRegel<T : Comparable<T>> : Regel<T>() {
    override fun kjør(opplysninger: List<Opplysning<*>>) { }
}

class EnAvRegel<T : Boolean>(
    private vararg val opplysningstyper: Opplysningstype<Boolean>,
) : Regel<T>(opplysningstyper.toList()) {
    override fun kjør(opplysninger: List<Opplysning<*>>) {
        val basertPå =
            opplysninger.filter {
                opplysningstyper.any { opplysningstype -> it.er(opplysningstype) }
            }

        return
    }
}
