package no.nav.dagpenger.behandling.regel

import no.nav.dagpenger.behandling.LesbarOpplysninger
import no.nav.dagpenger.behandling.Opplysningstype
import no.nav.dagpenger.behandling.Regelsett

internal class StørreEnn(
    produserer: Opplysningstype<Boolean>,
    private val a: Opplysningstype<Double>,
    private val b: Opplysningstype<Double>,
) : Regel<Boolean>(produserer, listOf(a, b)) {
    override fun kjør(opplysninger: LesbarOpplysninger): Boolean {
        val a = opplysninger.finnOpplysning(a).verdi
        val b = opplysninger.finnOpplysning(b).verdi
        return a > b
    }
}

fun Regelsett.størreEnn(
    produserer: Opplysningstype<Boolean>,
    er: Opplysningstype<Double>,
    størreEnn: Opplysningstype<Double>,
): Regel<Boolean> {
    return StørreEnn(produserer, er, størreEnn).also { leggTil(it) }
}
