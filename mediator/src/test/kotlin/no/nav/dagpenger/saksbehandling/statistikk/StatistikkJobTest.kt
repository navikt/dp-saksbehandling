package no.nav.dagpenger.saksbehandling.statistikk

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.assertions.json.shouldEqualSpecifiedJsonIgnoringOrder
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.statistikk.db.StatistikkTjeneste
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class StatistikkJobTest {
    private val testRapid = TestRapid()
    val nå = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
    val tilstandSøknadKlarTilBehandling =
        OppgaveITilstand(
            oppgaveId = UUIDv7.ny(),
            mottatt = LocalDateTime.now(),
            sakId = UUIDv7.ny(),
            behandlingId = UUIDv7.ny(),
            personIdent = "12345612345",
            saksbehandlerIdent = null,
            beslutterIdent = null,
            versjon = "dp:saksbehandling:1.2.3",
            tilstandsendring =
                OppgaveITilstand.Tilstandsendring(
                    sekvensnummer = 1,
                    tilstandsendringId = UUIDv7.ny(),
                    tilstand = "KLAR_TIL_BEHANDLING",
                    tidspunkt = nå.minusDays(1),
                ),
            utløstAv = "SØKNAD",
            behandlingResultat = null,
        )

    val tilstandSøknadUnderBehandling =
        OppgaveITilstand(
            oppgaveId = UUIDv7.ny(),
            mottatt = LocalDateTime.now(),
            sakId = UUIDv7.ny(),
            behandlingId = UUIDv7.ny(),
            personIdent = "12345612345",
            saksbehandlerIdent = "AB123",
            beslutterIdent = "B987",
            versjon = "dp:saksbehandling:1.2.3",
            tilstandsendring =
                OppgaveITilstand.Tilstandsendring(
                    sekvensnummer = 2,
                    tilstandsendringId = UUIDv7.ny(),
                    tilstand = "UNDER_BEHANDLING",
                    tidspunkt = nå,
                ),
            utløstAv = "SØKNAD",
            behandlingResultat = null,
        )

    val tilstandInnsendingFerdigBehandlet =
        OppgaveITilstand(
            oppgaveId = UUIDv7.ny(),
            mottatt = LocalDateTime.now(),
            sakId = UUIDv7.ny(),
            behandlingId = UUIDv7.ny(),
            personIdent = "12345612345",
            saksbehandlerIdent = "SB111",
            beslutterIdent = null,
            versjon = "dp:saksbehandling:1.2.3",
            tilstandsendring =
                OppgaveITilstand.Tilstandsendring(
                    sekvensnummer = 3,
                    tilstandsendringId = UUIDv7.ny(),
                    tilstand = "FERDIG_BEHANDLET",
                    tidspunkt = nå,
                ),
            utløstAv = "INNSENDING",
            behandlingResultat = "RettTilDagpenger",
        )
    private val statistikkTjeneste =
        mockk<StatistikkTjeneste>().also {
            every { it.tidligereTilstandsendringerErOverført() } returns true
            every { it.oppgaveTilstandsendringer() } returns
                listOf(
                    tilstandSøknadKlarTilBehandling,
                    tilstandSøknadUnderBehandling,
                    tilstandInnsendingFerdigBehandlet,
                )
            every { it.markerTilstandsendringerSomOverført(tilstandSøknadKlarTilBehandling.tilstandsendring.tilstandsendringId) } just Runs
            every { it.markerTilstandsendringerSomOverført(tilstandSøknadUnderBehandling.tilstandsendring.tilstandsendringId) } just Runs
            every { it.markerTilstandsendringerSomOverført(tilstandInnsendingFerdigBehandlet.tilstandsendring.tilstandsendringId) } just
                Runs
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
              "@event_name": "oppgave_til_statistikk",
              "oppgave": {
                "oppgaveId": "${tilstandSøknadKlarTilBehandling.oppgaveId}",
                "mottatt": "${tilstandSøknadKlarTilBehandling.mottatt}",
                "sakId": "${tilstandSøknadKlarTilBehandling.sakId}",
                "behandlingId": "${tilstandSøknadKlarTilBehandling.behandlingId}",
                "personIdent": "12345612345",
                "tilstandsendring": {
                  "sekvensnummer": 1,
                  "tilstandsendringId": "${tilstandSøknadKlarTilBehandling.tilstandsendring.tilstandsendringId}",
                  "tilstand": "KLAR_TIL_BEHANDLING",
                  "tidspunkt": "${tilstandSøknadKlarTilBehandling.tilstandsendring.tidspunkt}"
                },
                "utløstAv": "SØKNAD",
                "versjon": "dp:saksbehandling:1.2.3"
              }
            }
            """.trimIndent()
        testRapid.inspektør.message(1).toString() shouldEqualSpecifiedJsonIgnoringOrder
            """
            {
              "@event_name": "oppgave_til_statistikk",
              "oppgave": {
                "oppgaveId": "${tilstandSøknadUnderBehandling.oppgaveId}",
                "mottatt": "${tilstandSøknadUnderBehandling.mottatt}",
                "sakId": "${tilstandSøknadUnderBehandling.sakId}",
                "behandlingId": "${tilstandSøknadUnderBehandling.behandlingId}",
                "personIdent": "12345612345",
                "saksbehandlerIdent": "AB123",
                "beslutterIdent": "B987",
                "tilstandsendring": {
                  "sekvensnummer": 2,
                  "tilstandsendringId": "${tilstandSøknadUnderBehandling.tilstandsendring.tilstandsendringId}",
                  "tilstand": "UNDER_BEHANDLING",
                  "tidspunkt": "${tilstandSøknadUnderBehandling.tilstandsendring.tidspunkt}"
                },
                "utløstAv": "SØKNAD",
                "versjon": "dp:saksbehandling:1.2.3"
              }
            }
            """.trimIndent()
        testRapid.inspektør.message(2).toString() shouldEqualSpecifiedJsonIgnoringOrder
            """
            {
              "@event_name": "oppgave_til_statistikk",
              "oppgave": {
                "oppgaveId": "${tilstandInnsendingFerdigBehandlet.oppgaveId}",
                "mottatt": "${tilstandInnsendingFerdigBehandlet.mottatt}",
                "sakId": "${tilstandInnsendingFerdigBehandlet.sakId}",
                "behandlingId": "${tilstandInnsendingFerdigBehandlet.behandlingId}",
                "personIdent": "12345612345",
                "saksbehandlerIdent": "SB111",
                "tilstandsendring": {
                  "sekvensnummer": 3,
                  "tilstandsendringId": "${tilstandInnsendingFerdigBehandlet.tilstandsendring.tilstandsendringId}",
                  "tilstand": "FERDIG_BEHANDLET",
                  "tidspunkt": "${tilstandInnsendingFerdigBehandlet.tilstandsendring.tidspunkt}"
                },
                "utløstAv": "INNSENDING",
                "versjon": "dp:saksbehandling:1.2.3",
                "behandlingResultat": "RettTilDagpenger"
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
