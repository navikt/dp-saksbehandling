package no.nav.dagpenger.saksbehandling.api

import no.nav.dagpenger.saksbehandling.api.models.KlageGruppeDTO
import no.nav.dagpenger.saksbehandling.klage.Opplysning
import no.nav.dagpenger.saksbehandling.klage.OpplysningBygger.formkravOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningBygger.fristvurderingOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningBygger.fullmektigTilKlageinstansOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningBygger.klagenGjelderOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningBygger.oversittetFristOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningBygger.tilKlageinstansOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningBygger.utfallOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningType

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
                        OpplysningType.KLAGEN_GJELDER_VEDTAKSDATO,
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

    fun utfallOpplysninger(opplysninger: List<Opplysning>): List<Opplysning> {
        val utfallOpplysninger =
            opplysninger.filter { opplysning ->
                opplysning.type in
                    utfallOpplysningTyper + tilKlageinstansOpplysningTyper +
                    fullmektigTilKlageinstansOpplysningTyper
            }
        val sortedBy =
            utfallOpplysninger.sortedBy {
                val customOrder =
                    listOf(
                        OpplysningType.UTFALL,
                        OpplysningType.VURDERING_AV_KLAGEN,
                        OpplysningType.HVEM_KLAGER,
                        OpplysningType.FULLMEKTIG_NAVN,
                        OpplysningType.FULLMEKTIG_ADRESSE_1,
                        OpplysningType.FULLMEKTIG_ADRESSE_2,
                        OpplysningType.FULLMEKTIG_ADRESSE_3,
                        OpplysningType.FULLMEKTIG_POSTNR,
                        OpplysningType.FULLMEKTIG_POSTSTED,
                        OpplysningType.FULLMEKTIG_LAND,
                        OpplysningType.HJEMLER,
                        OpplysningType.INTERN_MELDING,
                    )
                customOrder.indexOf(it.type)
            }
        return sortedBy
    }
}
