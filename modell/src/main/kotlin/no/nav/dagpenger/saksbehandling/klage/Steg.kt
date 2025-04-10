package no.nav.dagpenger.saksbehandling.klage

import no.nav.dagpenger.saksbehandling.klage.OpplysningerBygger.formkravOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningerBygger.fristvurderingOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningerBygger.klagenGjelderOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningerBygger.oversittetFristOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningerBygger.utfallOpplysningTyper

interface Steg {
    fun evaluerSynlighet(opplysinger: Collection<Opplysning>)
}

class FristvurderingSteg : Steg {
    override fun evaluerSynlighet(opplysninger: Collection<Opplysning>) {
        when (klagefristOppfylt(opplysninger)) {
            true -> opplysninger.filter { it.type in oversittetFristOpplysningTyper }.forEach { it.settSynlighet(false) }
            false -> opplysninger.filter { it.type in oversittetFristOpplysningTyper }.forEach { it.settSynlighet(true) }
        }
    }

    private fun klagefristOppfylt(opplysinger: Collection<Opplysning>): Boolean {
        val klagefristOpplysning =
            opplysinger.single { opplysning -> opplysning.type == OpplysningType.KLAGEFRIST_OPPFYLT }
        return klagefristOpplysning.verdi is Verdi.Boolsk && (klagefristOpplysning.verdi as Verdi.Boolsk).value == true
    }
}

object FormkravSteg : Steg {
    override fun evaluerSynlighet(opplysinger: Collection<Opplysning>) {
    }
}

class VurderUtfallSteg : Steg {
    override fun evaluerSynlighet(opplysinger: Collection<Opplysning>) {
        val skjulUtfallOpplysninger =
            opplysinger.any {
                it.type in formkravOpplysningTyper + fristvurderingOpplysningTyper + oversittetFristOpplysningTyper +
                    klagenGjelderOpplysningTyper && it.synlighet() && it.verdi == Verdi.TomVerdi
            }
        when (skjulUtfallOpplysninger) {
            true -> opplysinger.filter { it.type in utfallOpplysningTyper }.forEach { it.settSynlighet(false) }
            false -> opplysinger.filter { it.type in utfallOpplysningTyper }.forEach { it.settSynlighet(true) }
        }
    }
}
