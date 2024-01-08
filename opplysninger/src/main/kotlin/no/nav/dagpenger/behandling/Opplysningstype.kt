package no.nav.dagpenger.behandling

interface Klassifiserbart {
    fun er(type: Opplysningstype): Boolean
}

class Opplysningstype(
    private val navn: String,
    private val parent: Opplysningstype? = null,
    private val bestårAv: MutableList<Opplysningstype> = mutableListOf(),
) : Klassifiserbart {
    init {
        parent?.bestårAv?.add(this)
    }

    fun bestårAv() = bestårAv.toList()

    override fun er(type: Opplysningstype): Boolean {
        return navn == type.navn || parent?.er(type) ?: false
    }
}
