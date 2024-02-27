package no.nav.dagpenger.saksbehandling.api

import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.api.models.OppgaveDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveTilstandDTO
import no.nav.dagpenger.saksbehandling.api.models.OpplysningDTO
import no.nav.dagpenger.saksbehandling.api.models.OpplysningTypeDTO
import no.nav.dagpenger.saksbehandling.api.models.StegDTO
import no.nav.dagpenger.saksbehandling.api.models.StegTilstandDTO
import no.nav.dagpenger.saksbehandling.api.models.SvarDTO
import java.time.LocalDate
import java.util.UUID

// TODO: Fjernes ettersom vi kommer lenger i utviklingen

internal val oppgaveFerdigBehandletUUID = UUID.fromString("7f9c2ac7-5bf2-46e6-a618-c1f4f85cd3f2")
internal val oppgaveTilBehandlingUUID = UUID.fromString("018d7964-347c-788b-aa97-8acaba091245")
internal val stegIdGjenopptak8Uker = UUID.fromString("8d936e88-b5fe-4e6b-96de-82a341494954")
internal val opplysningerGjenopptak8uker =
    listOf(
        OpplysningDTO(
            opplysningNavn = "Mulig gjenopptak",
            opplysningType = OpplysningTypeDTO.Boolean,
            svar = SvarDTO("false"),
        ),
        OpplysningDTO(
            opplysningNavn = "Har hatt lukkede saker siste 8 uker",
            opplysningType = OpplysningTypeDTO.Boolean,
            svar = SvarDTO("false"),
        ),
    )

internal val oppgaveTilBehandlingDTO =
    OppgaveDTO(
        oppgaveId = oppgaveTilBehandlingUUID,
        personIdent = "12345678901",
        datoOpprettet = LocalDate.now(),
        journalpostIder = listOf("12345678"),
        emneknagger = listOf("VurderAvslagPåMinsteinntekt"),
        tilstand = OppgaveTilstandDTO.TilBehandling,
        behandlingId = UUIDv7.ny(),
        steg =
            listOf(
                StegDTO(
                    stegNavn = "Gjenopptak / 8 uker",
                    opplysninger =
                        listOf(
                            OpplysningDTO(
                                opplysningNavn = "Mulig gjenopptak",
                                opplysningType = OpplysningTypeDTO.Boolean,
                                svar = null,
                            ),
                            OpplysningDTO(
                                opplysningNavn = "Har hatt lukkede saker siste 8 uker",
                                opplysningType = OpplysningTypeDTO.Boolean,
                                svar = null,
                            ),
                        ),
                ),
                StegDTO(
                    stegNavn = "Minste arbeidsinntekt",
                    opplysninger =
                        listOf(
                            OpplysningDTO(
                                opplysningNavn = "EØS-arbeid",
                                opplysningType = OpplysningTypeDTO.Boolean,
                                svar = null,
                            ),
                            OpplysningDTO(
                                opplysningNavn = "Jobb utenfor Norge",
                                opplysningType = OpplysningTypeDTO.Boolean,
                                svar = null,
                            ),
                            OpplysningDTO(
                                opplysningNavn = "Svangerskapsrelaterte sykepenger",
                                opplysningType = OpplysningTypeDTO.Boolean,
                                svar = null,
                            ),
                            OpplysningDTO(
                                opplysningNavn = "Det er inntekt neste kalendermåned",
                                opplysningType = OpplysningTypeDTO.Boolean,
                                svar = null,
                            ),
                        ),
                    tilstand = null,
                ),
            ),
    )

internal val oppgaveFerdigBehandletDTO =
    OppgaveDTO(
        oppgaveId = oppgaveFerdigBehandletUUID,
        personIdent = "12345678901",
        datoOpprettet = LocalDate.now(),
        journalpostIder = listOf("98989", "76767"),
        emneknagger = listOf("VurderAvslagPåMinsteinntekt"),
        tilstand = OppgaveTilstandDTO.FerdigBehandlet,
        behandlingId = UUIDv7.ny(),
        steg =
            listOf(
                StegDTO(
                    stegNavn = "Gjenopptak / 8 uker",
                    opplysninger = opplysningerGjenopptak8uker,
                    tilstand = StegTilstandDTO.Groenn,
                ),
                StegDTO(
                    stegNavn = "Minste arbeidsinntekt",
                    opplysninger =
                        listOf(
                            OpplysningDTO(
                                opplysningNavn = "EØS-arbeid",
                                opplysningType = OpplysningTypeDTO.Boolean,
                                svar = SvarDTO("false"),
                            ),
                            OpplysningDTO(
                                opplysningNavn = "Jobb utenfor Norge",
                                opplysningType = OpplysningTypeDTO.Boolean,
                                svar = SvarDTO("false"),
                            ),
                            OpplysningDTO(
                                opplysningNavn = "Svangerskapsrelaterte sykepenger",
                                opplysningType = OpplysningTypeDTO.Boolean,
                                svar = SvarDTO("false"),
                            ),
                            OpplysningDTO(
                                opplysningNavn = "Det er inntekt neste kalendermåned",
                                opplysningType = OpplysningTypeDTO.Boolean,
                                svar = SvarDTO("false"),
                            ),
                        ),
                    tilstand = StegTilstandDTO.Groenn,
                ),
            ),
    )

internal val oppgaveDtos = listOf(oppgaveTilBehandlingDTO, oppgaveFerdigBehandletDTO)
