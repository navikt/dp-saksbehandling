package no.nav.dagpenger.saksbehandling.klage

import no.nav.dagpenger.saksbehandling.klage.OpplysningerBygger.formkravOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningerBygger.fristvurderingOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningerBygger.klagenGjelserOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningerBygger.oversittetFristOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningerBygger.utfallOpplysningTyper

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
        val skalIkkeViseUtfallOpplysninger =
            opplysinger.any {
                it.type in formkravOpplysningTyper + fristvurderingOpplysningTyper + oversittetFristOpplysningTyper +
                    klagenGjelserOpplysningTyper && it.synlighet() && it.verdi == Verdi.TomVerdi
            }
        when (skalIkkeViseUtfallOpplysninger) {
            true -> opplysinger.filter { it.type in utfallOpplysningTyper }.forEach { it.settSynlighet(false) }
            false -> opplysinger.filter { it.type in utfallOpplysningTyper }.forEach { it.settSynlighet(true) }
        }
    }
}
