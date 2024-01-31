package no.nav.dagpenger.behandling.regel

import no.nav.dagpenger.behandling.Faktum
import no.nav.dagpenger.behandling.Hypotese
import no.nav.dagpenger.behandling.LesbarOpplysninger
import no.nav.dagpenger.behandling.Opplysning
import no.nav.dagpenger.behandling.Opplysningstype

abstract class Regel<T : Comparable<T>>(
    internal val produserer: Opplysningstype<T>,
    val avhengerAv: List<Opplysningstype<*>> = emptyList(),
) {
    fun kanKjøre(opplysninger: LesbarOpplysninger): Boolean = opplysninger.finnAlle(avhengerAv).size == avhengerAv.size

    protected abstract fun kjør(opplysninger: LesbarOpplysninger): T

    fun produserer(opplysningstype: Opplysningstype<*>) = produserer.er(opplysningstype)

    fun lagProdukt(opplysninger: LesbarOpplysninger): Opplysning<T> {
        val basertPåFaktum =
            opplysninger.finnAlle(avhengerAv).all {
                it is Faktum<*>
            }
        return when (basertPåFaktum) {
            true -> return Faktum(produserer, kjør(opplysninger))
            false -> Hypotese(produserer, kjør(opplysninger))
        }
    }
}
