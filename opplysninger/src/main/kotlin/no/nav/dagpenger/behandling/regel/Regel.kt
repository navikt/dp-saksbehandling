package no.nav.dagpenger.behandling.regel

import no.nav.dagpenger.behandling.Hypotese
import no.nav.dagpenger.behandling.Opplysning
import no.nav.dagpenger.behandling.Opplysningstype

abstract class Regel<T : Comparable<T>>(
    private val produserer: Opplysningstype<T>,
    val avhengerAv: List<Opplysningstype<*>> = emptyList(),
) {
    fun kanKjøre(opplysninger: List<Opplysning<*>>): Boolean =
        opplysninger.none { it.er(produserer) } &&
            avhengerAv.all { opplysninger.any { opplysning -> opplysning.er(it) } }

    protected abstract fun kjør(opplysninger: List<Opplysning<*>>): T

    fun blurp(opplysninger: List<Opplysning<*>>): Opplysning<T> {
        return Hypotese(produserer, kjør(opplysninger))
    }
}
