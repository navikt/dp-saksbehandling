package no.nav.dagpenger.saksbehandling.api

import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.api.KlageView.behandlingOpplysninger
import no.nav.dagpenger.saksbehandling.api.KlageView.finnGruppe
import no.nav.dagpenger.saksbehandling.api.KlageView.utfallOpplysninger
import no.nav.dagpenger.saksbehandling.api.models.AvbrytKlageAarsakDTO
import no.nav.dagpenger.saksbehandling.api.models.BoolskVerdiDTO
import no.nav.dagpenger.saksbehandling.api.models.DatoVerdiDTO
import no.nav.dagpenger.saksbehandling.api.models.KlageDTO
import no.nav.dagpenger.saksbehandling.api.models.KlageDTOTilstandDTO
import no.nav.dagpenger.saksbehandling.api.models.KlageOpplysningBoolskDTO
import no.nav.dagpenger.saksbehandling.api.models.KlageOpplysningDTO
import no.nav.dagpenger.saksbehandling.api.models.KlageOpplysningDatoDTO
import no.nav.dagpenger.saksbehandling.api.models.KlageOpplysningFlerListeValgDTO
import no.nav.dagpenger.saksbehandling.api.models.KlageOpplysningListeValgDTO
import no.nav.dagpenger.saksbehandling.api.models.KlageOpplysningTekstDTO
import no.nav.dagpenger.saksbehandling.api.models.KlageOpplysningUUIDDTO
import no.nav.dagpenger.saksbehandling.api.models.KlageinstansBehandlingDTO
import no.nav.dagpenger.saksbehandling.api.models.KlageinstansUtfallDTO
import no.nav.dagpenger.saksbehandling.api.models.KlageinstansUtfallDTOVerdiDTO
import no.nav.dagpenger.saksbehandling.api.models.ListeVerdiDTO
import no.nav.dagpenger.saksbehandling.api.models.OppdaterKlageOpplysningDTO
import no.nav.dagpenger.saksbehandling.api.models.TekstVerdiDTO
import no.nav.dagpenger.saksbehandling.api.models.UUIDVerdiDTO
import no.nav.dagpenger.saksbehandling.api.models.UtfallDTO
import no.nav.dagpenger.saksbehandling.api.models.UtfallDTOVerdiDTO
import no.nav.dagpenger.saksbehandling.klage.Datatype
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.BEHANDLES
import no.nav.dagpenger.saksbehandling.klage.Opplysning
import no.nav.dagpenger.saksbehandling.klage.UtfallType
import no.nav.dagpenger.saksbehandling.klage.Verdi

class KlageDTOMapper(
    private val oppslag: Oppslag,
) {
    fun tilVerdi(oppdaterKlageOpplysningDTO: OppdaterKlageOpplysningDTO): Verdi =
        when (oppdaterKlageOpplysningDTO) {
            is BoolskVerdiDTO -> Verdi.Boolsk(oppdaterKlageOpplysningDTO.verdi)
            is DatoVerdiDTO -> Verdi.Dato(oppdaterKlageOpplysningDTO.verdi)
            is ListeVerdiDTO -> Verdi.Flervalg(oppdaterKlageOpplysningDTO.verdi)
            is TekstVerdiDTO -> Verdi.TekstVerdi(oppdaterKlageOpplysningDTO.verdi)
            is UUIDVerdiDTO -> Verdi.UUID(oppdaterKlageOpplysningDTO.verdi)
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
            tilstand =
                when (klageBehandling.tilstand().type) {
                    BEHANDLES -> KlageDTOTilstandDTO.BEHANDLES
                    KlageBehandling.KlageTilstand.Type.BEHANDLING_UTFORT -> KlageDTOTilstandDTO.BEHANDLING_UTFORT
                    KlageBehandling.KlageTilstand.Type.OVERSEND_KLAGEINSTANS -> KlageDTOTilstandDTO.OVERSEND_KLAGEINSTANS
                    KlageBehandling.KlageTilstand.Type.BEHANDLES_AV_KLAGEINSTANS -> KlageDTOTilstandDTO.BEHANDLES_AV_KLAGEINSTANS
                    KlageBehandling.KlageTilstand.Type.FERDIGSTILT -> KlageDTOTilstandDTO.FERDIGSTILT
                    KlageBehandling.KlageTilstand.Type.AVBRUTT -> KlageDTOTilstandDTO.AVBRUTT
                },
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
            klageinstansBehandling =
                klageBehandling.klageinstansVedtak()?.let { klageinstansVedtak ->
                    val klageinstansUtfallDTO =
                        when (klageinstansVedtak.utfall()) {
                            "STADFESTELSE" ->
                                KlageinstansUtfallDTO(
                                    verdi = KlageinstansUtfallDTOVerdiDTO.STADFESTELSE,
                                    tilgjengeligeKlageinstansUtfall =
                                        KlageinstansUtfallDTOVerdiDTO.entries
                                            .map {
                                                it.value
                                            },
                                )
                            "MEDHOLD" ->
                                KlageinstansUtfallDTO(
                                    verdi = KlageinstansUtfallDTOVerdiDTO.MEDHOLD,
                                    tilgjengeligeKlageinstansUtfall =
                                        KlageinstansUtfallDTOVerdiDTO.entries
                                            .map {
                                                it.value
                                            },
                                )
                            "DELVIS_MEDHOLD" ->
                                KlageinstansUtfallDTO(
                                    verdi = KlageinstansUtfallDTOVerdiDTO.DELVIS_MEDHOLD,
                                    tilgjengeligeKlageinstansUtfall =
                                        KlageinstansUtfallDTOVerdiDTO.entries
                                            .map {
                                                it.value
                                            },
                                )
                            "TRUKKET" ->
                                KlageinstansUtfallDTO(
                                    verdi = KlageinstansUtfallDTOVerdiDTO.TRUKKET,
                                    tilgjengeligeKlageinstansUtfall =
                                        KlageinstansUtfallDTOVerdiDTO.entries
                                            .map {
                                                it.value
                                            },
                                )
                            "RETUR" ->
                                KlageinstansUtfallDTO(
                                    verdi = KlageinstansUtfallDTOVerdiDTO.RETUR,
                                    tilgjengeligeKlageinstansUtfall =
                                        KlageinstansUtfallDTOVerdiDTO.entries
                                            .map {
                                                it.value
                                            },
                                )
                            "OPPHEVET" ->
                                KlageinstansUtfallDTO(
                                    verdi = KlageinstansUtfallDTOVerdiDTO.OPPHEVET,
                                    tilgjengeligeKlageinstansUtfall =
                                        KlageinstansUtfallDTOVerdiDTO.entries
                                            .map {
                                                it.value
                                            },
                                )
                            "UGUNST" ->
                                KlageinstansUtfallDTO(
                                    verdi = KlageinstansUtfallDTOVerdiDTO.UGUNST,
                                    tilgjengeligeKlageinstansUtfall =
                                        KlageinstansUtfallDTOVerdiDTO.entries
                                            .map {
                                                it.value
                                            },
                                )
                            "AVVIST" ->
                                KlageinstansUtfallDTO(
                                    verdi = KlageinstansUtfallDTOVerdiDTO.AVVIST,
                                    tilgjengeligeKlageinstansUtfall =
                                        KlageinstansUtfallDTOVerdiDTO.entries
                                            .map {
                                                it.value
                                            },
                                )
                            "HENLAGT" ->
                                KlageinstansUtfallDTO(
                                    verdi = KlageinstansUtfallDTOVerdiDTO.HENLAGT,
                                    tilgjengeligeKlageinstansUtfall =
                                        KlageinstansUtfallDTOVerdiDTO.entries
                                            .map {
                                                it.value
                                            },
                                )
                            else -> null
                        }
                    val klageinstansUtfallDTONotNull = requireNotNull(klageinstansUtfallDTO)

                    KlageinstansBehandlingDTO(
                        behandlingId = klageinstansVedtak.id,
                        utfall = klageinstansUtfallDTONotNull,
                        journalpostIder = klageinstansVedtak.journalpostIder,
                    )
                },
            lovligeAvbrytAarsaker = klageBehandling.lovligeAvbrytÅrsaker(),
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

                Datatype.UUID -> {
                    KlageOpplysningUUIDDTO(
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
                                (opplysning.verdi() as Verdi.UUID).value
                            },
                    )
                }
            }
        }
}

internal fun KlageBehandling.lovligeAvbrytÅrsaker(): List<AvbrytKlageAarsakDTO> =
    when (this.tilstand().type) {
        BEHANDLES -> AvbrytKlageAarsakDTO.entries
        else -> emptyList()
    }
