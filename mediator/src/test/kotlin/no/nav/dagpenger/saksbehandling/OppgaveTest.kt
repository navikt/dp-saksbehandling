package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.api.config.objectMapper
import no.nav.dagpenger.saksbehandling.api.models.OppgaveDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveTilstandDTO
import no.nav.dagpenger.saksbehandling.api.oppgaveTilBehandlingDTO
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class OppgaveTest {
    @Test
    fun `SKal kunne serializere noe`() {
        OppgaveDTO(
            oppgaveId = UUIDv7.ny(),
            behandlingId = UUIDv7.ny(),
            personIdent = "adipiscing",
            tidspunktOpprettet = ZonedDateTime.now(),
            emneknagger = listOf(),
            tilstand = OppgaveTilstandDTO.FerdigBehandlet,
            steg = listOf(),
            journalpostIder = listOf(),
        ).let {
            objectMapper.writeValueAsString(it).also { json -> println(json) }
            objectMapper.writeValueAsString(oppgaveTilBehandlingDTO).also { json -> println(json) }
        }
    }
}
