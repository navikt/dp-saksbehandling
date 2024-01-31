package no.nav.dagpenger.behandling.regel

import no.nav.dagpenger.behandling.Opplysning
import no.nav.dagpenger.behandling.Opplysningstype
import no.nav.dagpenger.behandling.Regelsett
import java.time.LocalDate

internal class Oppslag<T : Comparable<T>>(
    produserer: Opplysningstype<T>,
    private val dato: Opplysningstype<LocalDate>,
    private val block: (LocalDate) -> T,
) : Regel<T>(produserer, listOf(dato)) {
    override fun kjør(opplysninger: List<Opplysning<*>>): T {
        val oppslagsdato =
            opplysninger.find { it.er(dato) }?.verdi as LocalDate

        return block(oppslagsdato)
    }

    override fun toString(): String {
        return "Oppslag av $produserer for $dato"
    }
}

fun <T : Comparable<T>> Regelsett.oppslag(
    produserer: Opplysningstype<T>,
    dato: Opplysningstype<LocalDate>,
    block: (LocalDate) -> T,
): Regel<T> {
    return Oppslag(produserer, dato, block).also { leggTil(it) }
}
