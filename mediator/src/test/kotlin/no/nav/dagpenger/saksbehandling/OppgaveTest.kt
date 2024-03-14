package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.api.config.objectMapper
import no.nav.dagpenger.saksbehandling.api.models.DataTypeDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveTilstandDTO
import no.nav.dagpenger.saksbehandling.api.models.OpplysningDTO
import no.nav.dagpenger.saksbehandling.api.models.OpplysningStatusDTO
import no.nav.dagpenger.saksbehandling.api.models.StegDTO
import no.nav.dagpenger.saksbehandling.api.models.StegTilstandDTO
import no.nav.dagpenger.saksbehandling.api.models.SvarDTO
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.UUID

class OppgaveTest {
    @Test
    fun `SKal kunne serializere noe`() {
        OppgaveDTO(
            oppgaveId = UUIDv7.ny(),
            behandlingId = UUIDv7.ny(),
            personIdent = "adipiscing",
            tidspunktOpprettet = ZonedDateTime.now(),
            emneknagger = listOf(),
            tilstand = OppgaveTilstandDTO.FERDIG_BEHANDLET,
            steg = listOf(),
            journalpostIder = listOf(),
        ).let {
            objectMapper.writeValueAsString(it).also { json -> println(json) }
            objectMapper.writeValueAsString(minsteinntektOppgaveTilBehandling).also { json -> println(json) }
        }
    }
}

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
