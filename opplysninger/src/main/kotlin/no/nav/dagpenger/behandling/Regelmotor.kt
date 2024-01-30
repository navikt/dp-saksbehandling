package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.regel.EnAvRegel
import no.nav.dagpenger.behandling.regel.Multiplikasjon
import no.nav.dagpenger.behandling.regel.Regel
import no.nav.dagpenger.behandling.regel.StørreEnn

class Regelmotor(
    private val regler: MutableMap<Opplysningstype<*>, Regel<*>> = mutableMapOf(),
) {
    private lateinit var opplysninger: Opplysninger

    fun registrer(opplysninger: Opplysninger) {
        this.opplysninger = opplysninger
    }

    fun kjør(opplysning: Opplysning<*>) {
        // TODO: Skriv om til EligibilityEngine fra DSL boka til Fowler
        val regelSomSkalKjøres = regler.filter { it.value.kanKjøre(opplysninger) }
        regelSomSkalKjøres.forEach {
            val verdi = it.value.blurp(opplysninger)
            opplysninger.leggTil(verdi)
            // TODO: Finn ut om opplysningen skal bekreftes til faktum (om den er basert på faktum)
        }
    }

    fun enAvRegel(
        produserer: Opplysningstype<Boolean>,
        vararg opplysningstype: Opplysningstype<Boolean>,
    ): Regel<Boolean> {
        return EnAvRegel(produserer, *opplysningstype).also { leggTil(produserer, it) }
    }

    fun multiplikasjon(
        produserer: Opplysningstype<Double>,
        vararg opplysningstype: Opplysningstype<Double>,
    ): Regel<Double> {
        return Multiplikasjon(produserer, *opplysningstype).also { leggTil(produserer, it) }
    }

    fun størreEnn(
        produserer: Opplysningstype<Boolean>,
        er: Opplysningstype<Double>,
        størreEnn: Opplysningstype<Double>,
    ): Regel<Boolean> {
        return StørreEnn(produserer, er, størreEnn).also { leggTil(produserer, it) }
    }

    private fun leggTil(
        produserer: Opplysningstype<*>,
        regel: Regel<*>,
    ) {
        if (regler.containsKey(produserer)) throw IllegalStateException("Regel for $produserer finnes allerede")
        produserer.utledesAv.addAll(regel.avhengerAv)
        regler[produserer] = regel
    }
}
