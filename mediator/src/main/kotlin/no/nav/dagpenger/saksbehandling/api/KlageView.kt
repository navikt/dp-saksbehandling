package no.nav.dagpenger.saksbehandling.api

import no.nav.dagpenger.saksbehandling.api.models.KlageGruppeDTO
import no.nav.dagpenger.saksbehandling.klage.Opplysning
import no.nav.dagpenger.saksbehandling.klage.OpplysningType
import no.nav.dagpenger.saksbehandling.klage.OpplysningerBygger.formkravOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningerBygger.fristvurderingOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningerBygger.fullmektigTilKlageinstansOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningerBygger.klagenGjelderOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningerBygger.oversittetFristOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningerBygger.tilKlageinstansOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningerBygger.utfallOpplysningTyper

object KlageView {
    fun finnGruppe(opplysningType: OpplysningType): KlageGruppeDTO {
        return when (opplysningType) {
            in klagenGjelderOpplysningTyper -> KlageGruppeDTO.KLAGESAK
            in fristvurderingOpplysningTyper + oversittetFristOpplysningTyper -> KlageGruppeDTO.FRIST
            in formkravOpplysningTyper -> KlageGruppeDTO.FORMKRAV
            in (utfallOpplysningTyper + tilKlageinstansOpplysningTyper + fullmektigTilKlageinstansOpplysningTyper) ->
                KlageGruppeDTO.KLAGE_ANKE

            else -> {
                throw IllegalStateException("KlageGruppeOpplysningType $opplysningType ikke støttet")
            }
        }
    }

    fun behandlingOpplysninger(opplysninger: List<Opplysning>): List<Opplysning> {
        val behandlingOpplysninger =
            opplysninger.filter {
                it.type in (
                        klagenGjelderOpplysningTyper + formkravOpplysningTyper +
                                fristvurderingOpplysningTyper + oversittetFristOpplysningTyper
                        )
            }

        val sortedBy =
            behandlingOpplysninger.sortedBy {
                val customOrder =
                    listOf(
                        OpplysningType.KLAGEN_GJELDER,
                        OpplysningType.KLAGEN_GJELDER_VEDTAK,
                        OpplysningType.KLAGEFRIST,
                        OpplysningType.KLAGE_MOTTATT,
                        OpplysningType.KLAGEFRIST_OPPFYLT,
                        OpplysningType.OPPREISNING_OVERSITTET_FRIST,
                        OpplysningType.OPPREISNING_OVERSITTET_FRIST_BEGRUNNELSE,
                        OpplysningType.ER_KLAGEN_SKRIFTLIG,
                        OpplysningType.ER_KLAGEN_UNDERSKREVET,
                        OpplysningType.KLAGEN_NEVNER_ENDRING,
                        OpplysningType.RETTSLIG_KLAGEINTERESSE,
                    )
                customOrder.indexOf(it.type)
            }
        return sortedBy
    }
}
