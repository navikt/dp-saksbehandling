package no.nav.dagpenger.saksbehandling.klage

import no.nav.dagpenger.saksbehandling.klage.OpplysningerBygger.formkravOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningerBygger.fristvurderingOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningerBygger.fullmektigTilKlageinstansOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningerBygger.klagenGjelderOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningerBygger.oversittetFristOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningerBygger.tilKlageinstansOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningerBygger.utfallOpplysningTyper

interface Steg {
    fun evaluerSynlighet(opplysinger: Collection<Opplysning>)
}

object KlagenGjelderSteg : Steg {
    override fun evaluerSynlighet(opplysinger: Collection<Opplysning>) {
    }
}

object FristvurderingSteg : Steg {
    override fun evaluerSynlighet(opplysninger: Collection<Opplysning>) {
        when (klagefristOppfylt(opplysninger)) {
            true ->
                opplysninger.filter { it.type in oversittetFristOpplysningTyper }.forEach {
                    it.settSynlighet(false)
                }
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

object VurderUtfallSteg : Steg {
    override fun evaluerSynlighet(opplysinger: Collection<Opplysning>) {
        val skjulUtfallOpplysninger =
            opplysinger.any { opplysning ->
                opplysning.type in
                    formkravOpplysningTyper +
                    fristvurderingOpplysningTyper +
                    oversittetFristOpplysningTyper +
                    klagenGjelderOpplysningTyper &&
                    opplysning.synlighet() &&
                    opplysning.verdi == Verdi.TomVerdi
            }
        when (skjulUtfallOpplysninger) {
            true ->
                opplysinger.filter { it.type in utfallOpplysningTyper }.forEach {
                    it.settSynlighet(false)
                }
            false -> opplysinger.filter { it.type in utfallOpplysningTyper }.forEach { it.settSynlighet(true) }
        }
    }
}

object OversendKlageinstansSteg : Steg {
    override fun evaluerSynlighet(opplysinger: Collection<Opplysning>) {
        val visOversendelseKlageinstans =
            opplysinger.any { opplysning ->
                opplysning.type == OpplysningType.UTFALL &&
                    opplysning.verdi is Verdi.TekstVerdi &&
                    (opplysning.verdi as Verdi.TekstVerdi).value == UtfallType.OPPRETTHOLDELSE.name
            }
        when (visOversendelseKlageinstans) {
            true -> opplysinger.filter { it.type in tilKlageinstansOpplysningTyper }.forEach { it.settSynlighet(true) }
            false ->
                opplysinger.filter { it.type in tilKlageinstansOpplysningTyper }.forEach {
                    it.settSynlighet(false)
                }
        }
    }
}

object FullmektigSteg : Steg {
    override fun evaluerSynlighet(opplysinger: Collection<Opplysning>) {
        val fullMektigKlager =
            opplysinger.any { opplysning ->
                opplysning.type == OpplysningType.HVEM_KLAGER &&
                    opplysning.verdi is Verdi.TekstVerdi &&
                    (opplysning.verdi as Verdi.TekstVerdi).value == HvemKlagerType.FULLMEKTIG.name
            }

        when (fullMektigKlager) {
            true -> opplysinger.filter { it.type in fullmektigTilKlageinstansOpplysningTyper }.forEach { it.settSynlighet(true) }
            false ->
                opplysinger.filter { it.type in fullmektigTilKlageinstansOpplysningTyper }.forEach {
                    it.settSynlighet(false)
                }
        }
    }
}
