package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.regel.Regel

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

    fun trenger(opplysningstype: Opplysningstype<*>): Set<Opplysningstype<*>> {
        return when (regler.containsKey(opplysningstype)) {
            false -> return emptySet()
            true ->
                regler[opplysningstype]!!.avhengerAv.map {
                    if (regler[it] != null) {
                        trenger(it)
                    } else {
                        setOf(it)
                    }
                }.flatten().filterNot { opplysninger.har(it) }.toSet()
        }
    }

    internal fun leggTil(
        produserer: Opplysningstype<*>,
        regel: Regel<*>,
    ) {
        if (regler.containsKey(produserer)) throw IllegalStateException("Regel for $produserer finnes allerede")
        regler[produserer] = regel
    }
}
