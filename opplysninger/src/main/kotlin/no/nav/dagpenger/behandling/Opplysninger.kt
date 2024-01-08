package no.nav.dagpenger.behandling

class Opplysninger(
    private val opplysninger: MutableList<Opplysning> = mutableListOf(),
) {
    val size: Int get() = opplysninger.size

    fun leggTil(opplysning: Opplysning) =
        opplysninger.add(opplysning).also {
            opplysning.avhengerAv().map {
                opplysninger.add(Etterlyst(it))
            }
        }

    fun har(opplysningstype: Opplysningstype) = opplysninger.any { it.er(opplysningstype) }
}
