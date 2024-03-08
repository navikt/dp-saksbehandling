package no.nav.dagpenger.saksbehandling.api

import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.api.models.DataTypeDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveTilstandDTO
import no.nav.dagpenger.saksbehandling.api.models.OpplysningDTO
import no.nav.dagpenger.saksbehandling.api.models.OpplysningStatusDTO
import no.nav.dagpenger.saksbehandling.api.models.StegDTO
import no.nav.dagpenger.saksbehandling.api.models.StegTilstandDTO
import no.nav.dagpenger.saksbehandling.api.models.SvarDTO
import java.time.ZonedDateTime
import java.util.UUID

internal val opplysningerGjenopptak8uker =
    listOf(
        OpplysningDTO(
            opplysningNavn = "Mulig gjenopptak",
            dataType = DataTypeDTO.Boolean,
            status = OpplysningStatusDTO.Hypotese,
            svar = SvarDTO("false"),
        ),
        OpplysningDTO(
            opplysningNavn = "Har hatt lukkede saker siste 8 uker",
            dataType = DataTypeDTO.Boolean,
            status = OpplysningStatusDTO.Hypotese,
            svar = SvarDTO("false"),
        ),
    )

internal val minsteinntektOppgaveTilBehandlingId = UUID.fromString("018d7964-347c-788b-aa97-8acaba091245")
val personIdent = "12345678901"
val behandlingId1 = UUIDv7.ny()
internal val minsteinntektOppgaveTilBehandling =
    OppgaveDTO(
        oppgaveId = minsteinntektOppgaveTilBehandlingId,
        personIdent = personIdent,
        tidspunktOpprettet = ZonedDateTime.now(),
        journalpostIder = listOf("12345678"),
        emneknagger = listOf("VurderAvslagPåMinsteinntekt"),
        tilstand = OppgaveTilstandDTO.KLAR_TIL_BEHANDLING,
        behandlingId = behandlingId1,
        steg = listOf(
            StegDTO(
                stegNavn = "Gjenopptak",
                opplysninger =
                listOf(
                    OpplysningDTO(
                        opplysningNavn = "Mulig gjenopptak",
                        dataType = DataTypeDTO.Boolean,
                        status = OpplysningStatusDTO.Hypotese,
                        svar = null,
                    ),
                    OpplysningDTO(
                        opplysningNavn = "Har hatt lukkede saker siste 8 uker",
                        dataType = DataTypeDTO.Boolean,
                        status = OpplysningStatusDTO.Hypotese,
                        svar = null,
                    ),
                ),
            ),
            StegDTO(
                stegNavn = "Minsteinntekt",
                opplysninger =
                listOf(
                    OpplysningDTO(
                        opplysningNavn = "EØS-arbeid",
                        dataType = DataTypeDTO.Boolean,
                        status = OpplysningStatusDTO.Faktum,
                        svar = SvarDTO("false"),
                    ),
                    OpplysningDTO(
                        opplysningNavn = "Jobb utenfor Norge",
                        dataType = DataTypeDTO.Boolean,
                        status = OpplysningStatusDTO.Faktum,
                        svar = SvarDTO("false"),
                    ),
                    OpplysningDTO(
                        opplysningNavn = "Svangerskapsrelaterte sykepenger",
                        dataType = DataTypeDTO.Boolean,
                        status = OpplysningStatusDTO.Hypotese,
                        svar = null,
                    ),
                    OpplysningDTO(
                        opplysningNavn = "Det er inntekt neste kalendermåned",
                        dataType = DataTypeDTO.Boolean,
                        status = OpplysningStatusDTO.Hypotese,
                        svar = null,
                    ),
                ),
                tilstand = StegTilstandDTO.Gul,
            ),
        ),
    )

internal val minsteinntektOppgaveFerdigBehandletId = UUID.fromString("7f9c2ac7-5bf2-46e6-a618-c1f4f85cd3f2")
val personIdent2 = "12345678902"
val behandlingId2 = UUIDv7.ny()
internal val minsteinntektOppgaveFerdigBehandlet =
    OppgaveDTO(
        oppgaveId = minsteinntektOppgaveFerdigBehandletId,
        personIdent = personIdent2,
        tidspunktOpprettet = ZonedDateTime.now(),
        journalpostIder = listOf("98989", "76767"),
        emneknagger = listOf("VurderAvslagPåMinsteinntekt"),
        tilstand = OppgaveTilstandDTO.FERDIG_BEHANDLET,
        behandlingId = behandlingId2,
        steg =
        listOf(
            StegDTO(
                stegNavn = "Gjenopptak",
                opplysninger = opplysningerGjenopptak8uker,
                tilstand = StegTilstandDTO.Groenn,
            ),
            StegDTO(
                stegNavn = "Minsteinntekt",
                opplysninger =
                listOf(
                    OpplysningDTO(
                        opplysningNavn = "EØS-arbeid",
                        dataType = DataTypeDTO.Boolean,
                        status = OpplysningStatusDTO.Faktum,
                        svar = SvarDTO("false"),
                    ),
                    OpplysningDTO(
                        opplysningNavn = "Jobb utenfor Norge",
                        dataType = DataTypeDTO.Boolean,
                        status = OpplysningStatusDTO.Faktum,
                        svar = SvarDTO("false"),
                    ),
                    OpplysningDTO(
                        opplysningNavn = "Svangerskapsrelaterte sykepenger",
                        dataType = DataTypeDTO.Boolean,
                        status = OpplysningStatusDTO.Faktum,
                        svar = SvarDTO("false"),
                    ),
                    OpplysningDTO(
                        opplysningNavn = "Det er inntekt neste kalendermåned",
                        dataType = DataTypeDTO.Boolean,
                        status = OpplysningStatusDTO.Faktum,
                        svar = SvarDTO("false"),
                    ),
                ),
                tilstand = StegTilstandDTO.Groenn,
            ),
        ),
    )

internal val oppgaveDtos =
    listOf(
        minsteinntektOppgaveTilBehandling,
        minsteinntektOppgaveFerdigBehandlet,
    )
