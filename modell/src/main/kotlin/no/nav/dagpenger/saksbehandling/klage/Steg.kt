package no.nav.dagpenger.saksbehandling.klage

import no.nav.dagpenger.saksbehandling.klage.OpplysningBygger.formkravOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningBygger.fristvurderingOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningBygger.fullmektigTilKlageinstansOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningBygger.klagenGjelderOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningBygger.oversittetFristOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningBygger.tilKlageinstansOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningBygger.utfallOpplysningTyper

interface Steg {
    fun evaluerSynlighet(opplysninger: Collection<Opplysning>)
}

object KlagenGjelderSteg : Steg {
    override fun evaluerSynlighet(opplysninger: Collection<Opplysning>) {
    }
}

object FristvurderingSteg : Steg {
    override fun evaluerSynlighet(opplysninger: Collection<Opplysning>) {
        when (klagefristOppfylt(opplysninger)) {
            true ->
                opplysninger.filter { it.type in oversittetFristOpplysningTyper }.forEach {
                    it.settSynlighet(false)
                }

            false ->
                opplysninger.filter { it.type in oversittetFristOpplysningTyper }
                    .forEach { it.settSynlighet(true) }
        }
    }

    private fun klagefristOppfylt(opplysinger: Collection<Opplysning>): Boolean {
        val klagefristOpplysning =
            opplysinger.single { opplysning -> opplysning.type == OpplysningType.KLAGEFRIST_OPPFYLT }
        return (klagefristOpplysning.verdi() is Verdi.Boolsk && (klagefristOpplysning.verdi() as Verdi.Boolsk).value) ||
            klagefristOpplysning.verdi() is Verdi.TomVerdi
    }
}

object FormkravSteg : Steg {
    override fun evaluerSynlighet(opplysninger: Collection<Opplysning>) {
    }
}

object VurderUtfallSteg : Steg {
    override fun evaluerSynlighet(opplysninger: Collection<Opplysning>) {
        val skjulUtfallOpplysninger =
            opplysninger.any { opplysning ->
                opplysning.type in
                    formkravOpplysningTyper +
                    fristvurderingOpplysningTyper +
                    oversittetFristOpplysningTyper +
                    klagenGjelderOpplysningTyper &&
                    opplysning.synlighet() &&
                    opplysning.type.påkrevd &&
                    opplysning.verdi() == Verdi.TomVerdi
            }
        when (skjulUtfallOpplysninger) {
            true ->
                opplysninger.filter { it.type in utfallOpplysningTyper }.forEach {
                    it.settSynlighet(false)
                }

            false -> opplysninger.filter { it.type in utfallOpplysningTyper }.forEach { it.settSynlighet(true) }
        }
    }
}

object OversendKlageinstansSteg : Steg {
    override fun evaluerSynlighet(opplysninger: Collection<Opplysning>) {
        val visOversendelseKlageinstans =
            opplysninger.any { opplysning ->
                opplysning.type == OpplysningType.UTFALL &&
                    opplysning.verdi() is Verdi.TekstVerdi &&
                    (opplysning.verdi() as Verdi.TekstVerdi).value == UtfallType.OPPRETTHOLDELSE.name
            }
        when (visOversendelseKlageinstans) {
            true -> opplysninger.filter { it.type in tilKlageinstansOpplysningTyper }.forEach { it.settSynlighet(true) }
            false ->
                opplysninger.filter { it.type in tilKlageinstansOpplysningTyper }.forEach {
                    it.settSynlighet(false)
                }
        }
    }
}

object FullmektigSteg : Steg {
    override fun evaluerSynlighet(opplysninger: Collection<Opplysning>) {
        val fullmektigKlager =
            opplysninger.any { opplysning ->
                opplysning.type == OpplysningType.HVEM_KLAGER &&
                    opplysning.verdi() is Verdi.TekstVerdi &&
                    (opplysning.verdi() as Verdi.TekstVerdi).value == HvemKlagerType.FULLMEKTIG.name
            }

        when (fullmektigKlager) {
            true ->
                opplysninger.filter { it.type in fullmektigTilKlageinstansOpplysningTyper }
                    .forEach { it.settSynlighet(true) }

            false ->
                opplysninger.filter { it.type in fullmektigTilKlageinstansOpplysningTyper }.forEach {
                    it.settSynlighet(false)
                }
        }
    }
}
