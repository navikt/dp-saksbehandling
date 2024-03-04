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

// TODO: Fjernes ettersom vi kommer lenger i utviklingen

internal val mockSøknadBehandlingId = UUID.fromString("018dc0e6-0be3-7f17-b410-08f2072ffcb1")
internal val mockSøknadOppgaveId = UUID.fromString("A684607D-16C8-4BCE-A5A8-38890009E0F7")
internal val mockSøknadOppgaveDto =
    OppgaveDTO(
        oppgaveId = mockSøknadOppgaveId,
        behandlingId = mockSøknadBehandlingId,
        personIdent = "11111199999",
        tidspunktOpprettet = ZonedDateTime.now(),
        emneknagger = listOf("Søknadsbehandling"),
        tilstand = OppgaveTilstandDTO.TilBehandling,
        steg = emptyList(),
        journalpostIder = emptyList(),
    )

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
internal val minsteinntektOppgaveTilBehandling =
    OppgaveDTO(
        oppgaveId = minsteinntektOppgaveTilBehandlingId,
        personIdent = "12345678901",
        tidspunktOpprettet = ZonedDateTime.now(),
        journalpostIder = listOf("12345678"),
        emneknagger = listOf("VurderAvslagPåMinsteinntekt"),
        tilstand = OppgaveTilstandDTO.TilBehandling,
        behandlingId = UUIDv7.ny(),
        steg =
            listOf(
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
internal val minsteinntektOppgaveFerdigBehandlet =
    OppgaveDTO(
        oppgaveId = minsteinntektOppgaveFerdigBehandletId,
        personIdent = "12345678901",
        tidspunktOpprettet = ZonedDateTime.now(),
        journalpostIder = listOf("98989", "76767"),
        emneknagger = listOf("VurderAvslagPåMinsteinntekt"),
        tilstand = OppgaveTilstandDTO.FerdigBehandlet,
        behandlingId = UUIDv7.ny(),
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
