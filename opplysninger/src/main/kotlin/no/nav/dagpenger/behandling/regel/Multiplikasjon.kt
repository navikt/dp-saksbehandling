package no.nav.dagpenger.behandling.regel

import no.nav.dagpenger.behandling.LesbarOpplysninger
import no.nav.dagpenger.behandling.Opplysningstype
import no.nav.dagpenger.behandling.Regelsett

internal class Multiplikasjon(
    produserer: Opplysningstype<Double>,
    private vararg val opplysningstyper: Opplysningstype<Double>,
) : Regel<Double>(produserer, opplysningstyper.toList()) {
    override fun kjør(opplysninger: LesbarOpplysninger): Double {
        val verdier = opplysninger.finnAlle(opplysningstyper.toList()).map { it.verdi as Double }
        return verdier.reduce { acc, d -> acc * d }
    }

    override fun toString(): String {
        return "Multiplikasjon av ${opplysningstyper.joinToString(", ")}"
    }
}

fun Regelsett.multiplikasjon(
    produserer: Opplysningstype<Double>,
    vararg opplysningstype: Opplysningstype<Double>,
): Regel<Double> {
    return Multiplikasjon(produserer, *opplysningstype).also { leggTil(it) }
}
