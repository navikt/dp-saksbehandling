package no.nav.dagpenger.behandling

class Opplysninger(
    private val regelmotor: Regelmotor,
    private val opplysninger: MutableList<Opplysning<*>> = mutableListOf(),
) : List<Opplysning<*>> by opplysninger {
    init {
        regelmotor.registrer(this)
    }

    fun leggTil(opplysning: Opplysning<*>) {
        opplysninger.add(opplysning)
        regelmotor.kjør(opplysning)
    }

    fun har(opplysningstype: Opplysningstype<*>) = opplysninger.any { it.er(opplysningstype) }

    // TODO: Flytt bestårAv() til regel(motor)
    fun trenger(opplysningstype: Opplysningstype<*>) = regelmotor.trenger(opplysningstype)
}
