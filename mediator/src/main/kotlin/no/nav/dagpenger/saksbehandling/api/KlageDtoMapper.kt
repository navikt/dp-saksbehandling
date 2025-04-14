package no.nav.dagpenger.saksbehandling.api

import no.nav.dagpenger.saksbehandling.OpplysningerVerdi
import no.nav.dagpenger.saksbehandling.api.KlageView.behandlingOpplysninger
import no.nav.dagpenger.saksbehandling.api.KlageView.finnGruppe
import no.nav.dagpenger.saksbehandling.api.models.BoolskVerdiDTO
import no.nav.dagpenger.saksbehandling.api.models.DatoVerdiDTO
import no.nav.dagpenger.saksbehandling.api.models.KlageDTO
import no.nav.dagpenger.saksbehandling.api.models.KlageOpplysningBoolskDTO
import no.nav.dagpenger.saksbehandling.api.models.KlageOpplysningDTO
import no.nav.dagpenger.saksbehandling.api.models.KlageOpplysningDatoDTO
import no.nav.dagpenger.saksbehandling.api.models.KlageOpplysningFlerListeValgDTO
import no.nav.dagpenger.saksbehandling.api.models.KlageOpplysningTekstDTO
import no.nav.dagpenger.saksbehandling.api.models.ListeVerdiDTO
import no.nav.dagpenger.saksbehandling.api.models.OppdaterKlageOpplysningDTO
import no.nav.dagpenger.saksbehandling.api.models.TekstVerdiDTO
import no.nav.dagpenger.saksbehandling.api.models.UtfallDTO
import no.nav.dagpenger.saksbehandling.api.models.UtfallDTOVerdiDTO
import no.nav.dagpenger.saksbehandling.klage.Datatype
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling
import no.nav.dagpenger.saksbehandling.klage.Opplysning
import no.nav.dagpenger.saksbehandling.klage.OpplysningerBygger.fullmektigTilKlageinstansOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningerBygger.tilKlageinstansOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningerBygger.utfallOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.UtfallType
import no.nav.dagpenger.saksbehandling.klage.Verdi

object KlageDtoMapper {
    fun OppdaterKlageOpplysningDTO.tilVerdi(): OpplysningerVerdi {
        return when (this) {
            is BoolskVerdiDTO -> OpplysningerVerdi.Boolsk(this.verdi)
            is DatoVerdiDTO -> OpplysningerVerdi.Dato(this.verdi)
            is ListeVerdiDTO -> OpplysningerVerdi.TekstListe(this.verdi)
            is TekstVerdiDTO -> OpplysningerVerdi.Tekst(this.verdi)
        }
    }

    fun utFallOpplysninger(opplysninger: List<Opplysning>): List<Opplysning> {
        return opplysninger.filter {
            it.type in (
                utfallOpplysningTyper + tilKlageinstansOpplysningTyper +
                    fullmektigTilKlageinstansOpplysningTyper
            )
        }
    }

    fun KlageBehandling.tilDto(): KlageDTO {
        val synligeOpplysninger = synligeOpplysninger().toList()
        return KlageDTO(
            id = this.id,
            // todo
            saksbehandler = null,
            behandlingOpplysninger = behandlingOpplysninger(synligeOpplysninger).klageOpplysningDTO(),
            utfallOpplysninger = utFallOpplysninger(synligeOpplysninger).klageOpplysningDTO(),
            utfall =
                UtfallDTO(
                    verdi =
                        when (this.utfall()) {
                            UtfallType.OPPRETTHOLDELSE -> UtfallDTOVerdiDTO.OPPRETTHOLDELSE
                            UtfallType.MEDHOLD -> UtfallDTOVerdiDTO.MEDHOLD
                            UtfallType.DELVIS_MEDHOLD -> UtfallDTOVerdiDTO.DELVIS_MEDHOLD
                            UtfallType.AVVIST -> UtfallDTOVerdiDTO.AVVIST
                            null -> UtfallDTOVerdiDTO.IKKE_SATT
                        },
                    tilgjeneligeUtfall = emptyList(),
                ),
            meldingOmVedtak = null,
        )
    }

    private fun List<Opplysning>.klageOpplysningDTO(): List<KlageOpplysningDTO> =
        map { opplysning ->
            when (opplysning.type.datatype) {
                Datatype.TEKST ->
                    KlageOpplysningTekstDTO(
                        id = opplysning.id,
                        navn = opplysning.type.navn,
                        paakrevd = true,
                        gruppe = finnGruppe(opplysning.type),
                        valgmuligheter = emptyList(),
                        redigerbar = true,
                        verdi =
                            if (opplysning.verdi is Verdi.TomVerdi) {
                                null
                            } else {
                                (opplysning.verdi as Verdi.TekstVerdi).value
                            },
                    )

                Datatype.DATO -> {
                    KlageOpplysningDatoDTO(
                        id = opplysning.id,
                        navn = opplysning.type.navn,
                        paakrevd = true,
                        gruppe = finnGruppe(opplysning.type),
                        valgmuligheter = emptyList(),
                        redigerbar = true,
                        verdi =
                            if (opplysning.verdi is Verdi.TomVerdi) {
                                null
                            } else {
                                (opplysning.verdi as Verdi.Dato).value
                            },
                    )
                }

                Datatype.BOOLSK -> {
                    KlageOpplysningBoolskDTO(
                        id = opplysning.id,
                        navn = opplysning.type.navn,
                        paakrevd = true,
                        gruppe = finnGruppe(opplysning.type),
                        valgmuligheter = emptyList(),
                        redigerbar = true,
                        verdi =
                            if (opplysning.verdi is Verdi.TomVerdi) {
                                null
                            } else {
                                (opplysning.verdi as Verdi.Boolsk).value
                            },
                    )
                }

                Datatype.FLERVALG -> {
                    KlageOpplysningFlerListeValgDTO(
                        id = opplysning.id,
                        navn = opplysning.type.navn,
                        paakrevd = true,
                        gruppe = finnGruppe(opplysning.type),
                        valgmuligheter = emptyList(),
                        redigerbar = true,
                        verdi =
                            if (opplysning.verdi is Verdi.TomVerdi) {
                                null
                            } else {
                                (opplysning.verdi as Verdi.Flervalg).value
                            },
                    )
                }
            }
        }
}
