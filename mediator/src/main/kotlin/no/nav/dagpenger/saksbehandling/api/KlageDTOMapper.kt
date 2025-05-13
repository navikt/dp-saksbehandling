package no.nav.dagpenger.saksbehandling.api

import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.api.KlageView.behandlingOpplysninger
import no.nav.dagpenger.saksbehandling.api.KlageView.finnGruppe
import no.nav.dagpenger.saksbehandling.api.KlageView.utfallOpplysninger
import no.nav.dagpenger.saksbehandling.api.models.BoolskVerdiDTO
import no.nav.dagpenger.saksbehandling.api.models.DatoVerdiDTO
import no.nav.dagpenger.saksbehandling.api.models.KlageDTO
import no.nav.dagpenger.saksbehandling.api.models.KlageOpplysningBoolskDTO
import no.nav.dagpenger.saksbehandling.api.models.KlageOpplysningDTO
import no.nav.dagpenger.saksbehandling.api.models.KlageOpplysningDatoDTO
import no.nav.dagpenger.saksbehandling.api.models.KlageOpplysningFlerListeValgDTO
import no.nav.dagpenger.saksbehandling.api.models.KlageOpplysningListeValgDTO
import no.nav.dagpenger.saksbehandling.api.models.KlageOpplysningTekstDTO
import no.nav.dagpenger.saksbehandling.api.models.ListeVerdiDTO
import no.nav.dagpenger.saksbehandling.api.models.MeldingOmVedtakResponseDTO
import no.nav.dagpenger.saksbehandling.api.models.OppdaterKlageOpplysningDTO
import no.nav.dagpenger.saksbehandling.api.models.TekstVerdiDTO
import no.nav.dagpenger.saksbehandling.api.models.UtfallDTO
import no.nav.dagpenger.saksbehandling.api.models.UtfallDTOVerdiDTO
import no.nav.dagpenger.saksbehandling.klage.Datatype
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling
import no.nav.dagpenger.saksbehandling.klage.Opplysning
import no.nav.dagpenger.saksbehandling.klage.UtfallType
import no.nav.dagpenger.saksbehandling.klage.Verdi

class KlageDTOMapper(private val oppslag: Oppslag) {
    fun tilVerdi(oppdaterKlageOpplysningDTO: OppdaterKlageOpplysningDTO): Verdi {
        return when (oppdaterKlageOpplysningDTO) {
            is BoolskVerdiDTO -> Verdi.Boolsk(oppdaterKlageOpplysningDTO.verdi)
            is DatoVerdiDTO -> Verdi.Dato(oppdaterKlageOpplysningDTO.verdi)
            is ListeVerdiDTO -> Verdi.Flervalg(oppdaterKlageOpplysningDTO.verdi)
            is TekstVerdiDTO -> Verdi.TekstVerdi(oppdaterKlageOpplysningDTO.verdi)
        }
    }

    suspend fun tilDto(
        klageBehandling: KlageBehandling,
        saksbehandler: Saksbehandler,
    ): KlageDTO {
        val synligeOpplysninger = klageBehandling.synligeOpplysninger().toList()
        return KlageDTO(
            behandlingId = klageBehandling.behandlingId,
            saksbehandler = oppslag.hentBehandler(saksbehandler.navIdent),
            behandlingOpplysninger = behandlingOpplysninger(synligeOpplysninger).klageOpplysningDTO(),
            utfallOpplysninger = utfallOpplysninger(synligeOpplysninger).klageOpplysningDTO(),
            utfall =
                UtfallDTO(
                    verdi =
                        when (klageBehandling.utfall()) {
                            UtfallType.OPPRETTHOLDELSE -> UtfallDTOVerdiDTO.OPPRETTHOLDELSE
                            UtfallType.MEDHOLD -> UtfallDTOVerdiDTO.MEDHOLD
                            UtfallType.DELVIS_MEDHOLD -> UtfallDTOVerdiDTO.DELVIS_MEDHOLD
                            UtfallType.AVVIST -> UtfallDTOVerdiDTO.AVVIST
                            null -> UtfallDTOVerdiDTO.IKKE_SATT
                        },
                    tilgjengeligeUtfall =
                        UtfallDTOVerdiDTO.entries
                            .filterNot { it == UtfallDTOVerdiDTO.IKKE_SATT }
                            .map { it.value },
                ),
            meldingOmVedtak =
                MeldingOmVedtakResponseDTO(
                    html = "<html><h1>Hei</H1></html>",
                    utvidedeBeskrivelser = emptyList(),
                ),
        )
    }

    private fun List<Opplysning>.klageOpplysningDTO(): List<KlageOpplysningDTO> =
        map { opplysning ->
            when (opplysning.type.datatype) {
                Datatype.TEKST ->
                    when (opplysning.valgmuligheter.isEmpty()) {
                        true -> {
                            KlageOpplysningTekstDTO(
                                opplysningId = opplysning.opplysningId,
                                opplysningNavnId = opplysning.type.name,
                                navn = opplysning.type.navn,
                                paakrevd = opplysning.type.påkrevd,
                                gruppe = finnGruppe(opplysning.type),
                                valgmuligheter = opplysning.valgmuligheter,
                                redigerbar = true,
                                verdi =
                                    if (opplysning.verdi() is Verdi.TomVerdi) {
                                        null
                                    } else {
                                        (opplysning.verdi() as Verdi.TekstVerdi).value
                                    },
                            )
                        }
                        false -> {
                            KlageOpplysningListeValgDTO(
                                opplysningId = opplysning.opplysningId,
                                navn = opplysning.type.navn,
                                opplysningNavnId = opplysning.type.name,
                                paakrevd = opplysning.type.påkrevd,
                                gruppe = finnGruppe(opplysning.type),
                                valgmuligheter = opplysning.valgmuligheter,
                                redigerbar = true,
                                verdi =
                                    if (opplysning.verdi() is Verdi.TomVerdi) {
                                        null
                                    } else {
                                        (opplysning.verdi() as Verdi.TekstVerdi).value
                                    },
                            )
                        }
                    }

                Datatype.DATO -> {
                    KlageOpplysningDatoDTO(
                        opplysningId = opplysning.opplysningId,
                        navn = opplysning.type.navn,
                        opplysningNavnId = opplysning.type.name,
                        paakrevd = opplysning.type.påkrevd,
                        gruppe = finnGruppe(opplysning.type),
                        valgmuligheter = opplysning.valgmuligheter,
                        redigerbar = true,
                        verdi =
                            if (opplysning.verdi() is Verdi.TomVerdi) {
                                null
                            } else {
                                (opplysning.verdi() as Verdi.Dato).value
                            },
                    )
                }

                Datatype.BOOLSK -> {
                    KlageOpplysningBoolskDTO(
                        opplysningId = opplysning.opplysningId,
                        navn = opplysning.type.navn,
                        opplysningNavnId = opplysning.type.name,
                        paakrevd = opplysning.type.påkrevd,
                        gruppe = finnGruppe(opplysning.type),
                        valgmuligheter = opplysning.valgmuligheter,
                        redigerbar = true,
                        verdi =
                            if (opplysning.verdi() is Verdi.TomVerdi) {
                                null
                            } else {
                                (opplysning.verdi() as Verdi.Boolsk).value
                            },
                    )
                }

                Datatype.FLERVALG -> {
                    KlageOpplysningFlerListeValgDTO(
                        opplysningId = opplysning.opplysningId,
                        navn = opplysning.type.navn,
                        opplysningNavnId = opplysning.type.name,
                        paakrevd = opplysning.type.påkrevd,
                        gruppe = finnGruppe(opplysning.type),
                        valgmuligheter = opplysning.valgmuligheter,
                        redigerbar = true,
                        verdi =
                            if (opplysning.verdi() is Verdi.TomVerdi) {
                                null
                            } else {
                                (opplysning.verdi() as Verdi.Flervalg).value
                            },
                    )
                }
            }
        }
}
