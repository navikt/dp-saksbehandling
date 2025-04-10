package no.nav.dagpenger.saksbehandling.klage

import no.nav.dagpenger.saksbehandling.klage.OpplysningerBygger.oversittetFristOpplysningTyper

interface Steg {
    fun reevaluerOpplysninger(opplysinger: List<Opplysning>)
}

class FristvurderingSteg : Steg {
    override fun reevaluerOpplysninger(opplysninger: List<Opplysning>) {
        when (klagefristOppfylt(opplysninger)) {
            true -> opplysninger.filter { it.type in oversittetFristOpplysningTyper }.forEach { it.settSynlighet(false) }
            false -> {
                opplysninger.filter { it.type in oversittetFristOpplysningTyper }.forEach { it.settSynlighet(true) }
            }
        }
    }

    private fun klagefristOppfylt(opplysinger: List<Opplysning>): Boolean {
        val klagefristOpplysning =
            opplysinger.single { opplysning -> opplysning.type == OpplysningType.KLAGEFRIST_OPPFYLT }
        return klagefristOpplysning.verdi is Verdi.Boolsk && (klagefristOpplysning.verdi as Verdi.Boolsk).value == true
    }
}

object FormkravSteg : Steg {
    override fun reevaluerOpplysninger(opplysinger: List<Opplysning>) {
    }
}

class VurderUtfallSteg : Steg {
    override fun reevaluerOpplysninger(opplysinger: List<Opplysning>) {
    }
}
