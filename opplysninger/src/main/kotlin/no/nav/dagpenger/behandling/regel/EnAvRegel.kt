package no.nav.dagpenger.behandling.regel

import no.nav.dagpenger.behandling.Opplysning
import no.nav.dagpenger.behandling.Opplysningstype

class EnAvRegel(
    produserer: Opplysningstype<Boolean>,
    private vararg val opplysningstyper: Opplysningstype<Boolean>,
) : Regel<Boolean>(produserer, opplysningstyper.toList()) {
    override fun kjør(opplysninger: List<Opplysning<*>>): Boolean {
        return opplysningstyper.any { finn -> opplysninger.filter { it.er(finn) }.any { it.verdi as Boolean } }
    }
}
