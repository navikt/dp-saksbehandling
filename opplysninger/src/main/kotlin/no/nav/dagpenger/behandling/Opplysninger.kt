package no.nav.dagpenger.behandling

class Opplysninger(
    private val opplysninger: MutableList<Opplysning<*>> = mutableListOf(),
) : List<Opplysning<*>> by opplysninger {
    fun leggTil(opplysning: Opplysning<*>) =
        opplysninger.add(opplysning).also {
            opplysning.opplysningstype.regel.kjør(opplysninger.toList())
        }

    fun har(opplysningstype: Opplysningstype<*>) = opplysninger.any { it.er(opplysningstype) }

    fun trenger(opplysningstype: Opplysningstype<*>) = opplysningstype.bestårAv().filter { !har(it) }
}
