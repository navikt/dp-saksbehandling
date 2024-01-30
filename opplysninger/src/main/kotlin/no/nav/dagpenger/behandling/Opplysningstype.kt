package no.nav.dagpenger.behandling

interface Klassifiserbart {
    fun er(type: Opplysningstype<*>): Boolean
}

class Opplysningstype<T : Comparable<T>>(
    private val navn: String,
    private val parent: Opplysningstype<T>? = null,
    private val child: MutableSet<Opplysningstype<*>> = mutableSetOf(),
    val utledesAv: MutableSet<Opplysningstype<*>> = mutableSetOf(),
) : Klassifiserbart {
    init {
        parent?.child?.add(this)
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
