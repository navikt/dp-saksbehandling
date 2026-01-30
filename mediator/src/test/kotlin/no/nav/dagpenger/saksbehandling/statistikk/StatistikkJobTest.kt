package no.nav.dagpenger.saksbehandling.statistikk

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.assertions.json.shouldEqualSpecifiedJsonIgnoringOrder
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.UUIDv7
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class StatistikkJobTest {
    private val testRapid = TestRapid()
    val nå = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
    val tilstandKlarTilBehandling =
        OppgaveTilstandsendring(
            oppgaveId = UUIDv7.ny(),
            mottatt = LocalDate.now(),
            sakId = UUIDv7.ny(),
            behandlingId = UUIDv7.ny(),
            personIdent = "12345612345",
            saksbehandlerIdent = null,
            beslutterIdent = null,
            versjon = "dp:saksbehandling:1.2.3",
            tilstandsendring =
                OppgaveTilstandsendring.StatistikkOppgaveTilstandsendring(
                    id = UUIDv7.ny(),
                    tilstand = "KLAR_TIL_BEHANDLING",
                    tidspunkt = nå.minusDays(1),
                ),
            utløstAv = "SØKNAD",
        )

    val tilstandUnderBehandling =
        OppgaveTilstandsendring(
            oppgaveId = UUIDv7.ny(),
            mottatt = LocalDate.now(),
            sakId = UUIDv7.ny(),
            behandlingId = UUIDv7.ny(),
            personIdent = "12345612345",
            saksbehandlerIdent = "AB123",
            beslutterIdent = "B987",
            versjon = "dp:saksbehandling:1.2.3",
            tilstandsendring =
                OppgaveTilstandsendring.StatistikkOppgaveTilstandsendring(
                    id = UUIDv7.ny(),
                    tilstand = "UNDER_BEHANDLING",
                    tidspunkt = nå,
                ),
            utløstAv = "SØKNAD",
        )
    private val statistikkTjeneste =
        mockk<StatistikkTjeneste>().also {
            every { it.tidligereTilstandsendringerErOverført() } returns true
            every { it.oppgaveTilstandsendringer() } returns
                listOf(
                    tilstandKlarTilBehandling,
                    tilstandUnderBehandling,
                )
            every { it.markerTilstandsendringerSomOverført(tilstandKlarTilBehandling.tilstandsendring.id) } just Runs
            every { it.markerTilstandsendringerSomOverført(tilstandUnderBehandling.tilstandsendring.id) } just Runs
        }

    @Test
    fun `Skal publisere oppgavetilstandsendringer til statistikk på riktig format og sette de som publisert`() {
        runBlocking {
            StatistikkJob(
                rapidsConnection = testRapid,
                statistikkTjeneste = statistikkTjeneste,
            ).executeJob()
        }

        testRapid.inspektør.message(0).toString() shouldEqualSpecifiedJsonIgnoringOrder
            """
            {
              "@event_name": "oppgave_til_statistikk_v3",
              "oppgave": {
                "oppgaveId": "${tilstandKlarTilBehandling.oppgaveId}",
                "mottatt": "${tilstandKlarTilBehandling.mottatt}",
                "sakId": "${tilstandKlarTilBehandling.sakId}",
                "behandlingId": "${tilstandKlarTilBehandling.behandlingId}",
                "personIdent": "12345612345",
                "tilstandsendring": {
                  "id": "${tilstandKlarTilBehandling.tilstandsendring.id}",
                  "tilstand": "KLAR_TIL_BEHANDLING",
                  "tidspunkt": "${tilstandKlarTilBehandling.tilstandsendring.tidspunkt}"
                },
                "utløstAv": "SØKNAD",
                "versjon": "dp:saksbehandling:1.2.3"
              }
            }
            """.trimIndent()
        testRapid.inspektør.message(1).toString() shouldEqualSpecifiedJsonIgnoringOrder
            """
            {
              "@event_name": "oppgave_til_statistikk_v3",
              "oppgave": {
                "oppgaveId": "${tilstandUnderBehandling.oppgaveId}",
                "mottatt": "${tilstandUnderBehandling.mottatt}",
                "sakId": "${tilstandUnderBehandling.sakId}",
                "behandlingId": "${tilstandUnderBehandling.behandlingId}",
                "personIdent": "12345612345",
                "tilstandsendring": {
                  "id": "${tilstandUnderBehandling.tilstandsendring.id}",
                  "tilstand": "UNDER_BEHANDLING",
                  "tidspunkt": "${tilstandUnderBehandling.tilstandsendring.tidspunkt}"
                },
                "utløstAv": "SØKNAD",
                "versjon": "dp:saksbehandling:1.2.3"
              }
            }
            """.trimIndent()
    }

    @Test
    fun `Skal ikke publisere oppgavetilstandsendringer til statistikk hvis tidligere kjøring ikke er fullført`() {
        every { statistikkTjeneste.tidligereTilstandsendringerErOverført() } returns false

        runBlocking {
            StatistikkJob(
                rapidsConnection = testRapid,
                statistikkTjeneste = statistikkTjeneste,
            ).executeJob()
        }
        assert(testRapid.inspektør.size == 0)
    }
}
