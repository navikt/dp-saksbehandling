package no.nav.dagpenger.behandling.regel

import no.nav.dagpenger.behandling.Opplysning
import no.nav.dagpenger.behandling.Opplysningstype
import no.nav.dagpenger.behandling.Regelsett

internal class Multiplikasjon(
    produserer: Opplysningstype<Double>,
    private vararg val opplysningstyper: Opplysningstype<Double>,
) : Regel<Double>(produserer, opplysningstyper.toList()) {
    override fun kjør(opplysninger: List<Opplysning<*>>): Double {
        val verdier =
            opplysningstyper.filter { opplysningstype ->
                opplysninger.any { it.er(opplysningstype) }
            }.map { opplysningstype ->
                opplysninger.find { it.er(opplysningstype) }?.verdi as Double
            }

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
