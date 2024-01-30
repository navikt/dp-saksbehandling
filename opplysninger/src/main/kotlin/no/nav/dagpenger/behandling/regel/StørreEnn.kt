package no.nav.dagpenger.behandling.regel

import no.nav.dagpenger.behandling.Opplysning
import no.nav.dagpenger.behandling.Opplysningstype
import no.nav.dagpenger.behandling.Regelsett

class StørreEnn(
    produserer: Opplysningstype<Boolean>,
    private val a: Opplysningstype<Double>,
    private val b: Opplysningstype<Double>,
) : Regel<Boolean>(produserer, listOf(a, b)) {
    override fun kjør(opplysninger: List<Opplysning<*>>): Boolean {
        val verdi =
            opplysninger.filter { it.er(a) || it.er(b) }.let { opplysninger ->
                val a = opplysninger.find { it.er(a) }?.verdi as Double
                val b = opplysninger.find { it.er(b) }?.verdi as Double

                a > b
            }
        return verdi
    }
}

fun Regelsett.størreEnn(
    produserer: Opplysningstype<Boolean>,
    er: Opplysningstype<Double>,
    størreEnn: Opplysningstype<Double>,
): Regel<Boolean> {
    return StørreEnn(produserer, er, størreEnn).also { leggTil(it) }
}
