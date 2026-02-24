package no.nav.dagpenger.saksbehandling.statistikk

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.assertions.json.shouldEqualSpecifiedJsonIgnoringOrder
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.statistikk.db.SaksbehandlingsstatistikkRepository
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val ISO_TIMESTAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

class StatistikkJobTest {
    private val testRapid = TestRapid()
    val mottatt = LocalDateTime.of(2026, 1, 11, 9, 11, 30)
    val søknadKlarTilBehandling =
        OppgaveITilstand(
            oppgaveId = UUIDv7.ny(),
            mottatt = mottatt,
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
                    tidspunkt = mottatt.minusDays(1),
                ),
            utløstAv = "SØKNAD",
            behandlingResultat = null,
            fagsystem = null,
            behandlingÅrsak = null,
            arenaSakId = null,
            resultatBegrunnelse = null,
        )

    val søknadAvbrutt =
        OppgaveITilstand(
            oppgaveId = UUIDv7.ny(),
            mottatt = mottatt,
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
                    tilstand = "AVBRUTT_MANUELT",
                    tidspunkt = mottatt,
                ),
            utløstAv = "SØKNAD",
            behandlingResultat = "AVBRUTT",
            behandlingÅrsak = "BEHANDLES_I_ARENA",
            fagsystem = "ARENA",
            arenaSakId = "123",
            resultatBegrunnelse = null,
        )

    val innsendingFerdigBehandlet =
        OppgaveITilstand(
            oppgaveId = UUIDv7.ny(),
            mottatt = mottatt,
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
                    tidspunkt = mottatt,
                ),
            utløstAv = "INNSENDING",
            behandlingResultat = "RettTilDagpenger",
            behandlingÅrsak = "Årsak",
            fagsystem = "DAGPENGER",
            arenaSakId = null,
            resultatBegrunnelse = null,
        )

    val oppgavePåVent =
        OppgaveITilstand(
            oppgaveId = UUIDv7.ny(),
            mottatt = mottatt,
            sakId = UUIDv7.ny(),
            behandlingId = UUIDv7.ny(),
            personIdent = "12345612345",
            saksbehandlerIdent = null,
            beslutterIdent = null,
            versjon = "dp:saksbehandling:1.2.3",
            tilstandsendring =
                OppgaveITilstand.Tilstandsendring(
                    sekvensnummer = 4,
                    tilstandsendringId = UUIDv7.ny(),
                    tilstand = "PAA_VENT",
                    tidspunkt = mottatt.minusDays(1),
                ),
            utløstAv = "SØKNAD",
            behandlingResultat = null,
            fagsystem = null,
            behandlingÅrsak = null,
            arenaSakId = null,
            resultatBegrunnelse = "AVVENT_MELDEKORT",
        )
    private val saksbehandlingsstatistikkRepository =
        mockk<SaksbehandlingsstatistikkRepository>().also {
            every { it.tidligereTilstandsendringerErOverført() } returns true
            every { it.oppgaveTilstandsendringer() } returns
                listOf(
                    søknadKlarTilBehandling,
                    søknadAvbrutt,
                    innsendingFerdigBehandlet,
                    oppgavePåVent,
                )
            every { it.markerTilstandsendringerSomOverført(søknadKlarTilBehandling.tilstandsendring.tilstandsendringId) } just Runs
            every { it.markerTilstandsendringerSomOverført(søknadAvbrutt.tilstandsendring.tilstandsendringId) } just Runs
            every { it.markerTilstandsendringerSomOverført(oppgavePåVent.tilstandsendring.tilstandsendringId) } just Runs
            every { it.markerTilstandsendringerSomOverført(innsendingFerdigBehandlet.tilstandsendring.tilstandsendringId) } just
                Runs
        }

    @Test
    fun `Skal publisere oppgavetilstandsendringer til statistikk på riktig format og sette de som publisert`() {
        runBlocking {
            StatistikkJob(
                rapidsConnection = testRapid,
                saksbehandlingsstatistikkRepository = saksbehandlingsstatistikkRepository,
            ).executeJob()
        }

        testRapid.inspektør.message(0).toString() shouldEqualSpecifiedJsonIgnoringOrder
            """
            {
              "@event_name": "oppgave_til_statistikk_v5",
              "oppgave": {
                "oppgaveId": "${søknadKlarTilBehandling.oppgaveId}",
                "mottatt": "${søknadKlarTilBehandling.mottatt.format(ISO_TIMESTAMP)}",
                "sakId": "${søknadKlarTilBehandling.sakId}",
                "behandlingId": "${søknadKlarTilBehandling.behandlingId}",
                "personIdent": "12345612345",
                "tilstandsendring": {
                  "sekvensnummer": 1,
                  "tilstandsendringId": "${søknadKlarTilBehandling.tilstandsendring.tilstandsendringId}",
                  "tilstand": "KLAR_TIL_BEHANDLING",
                  "tidspunkt": "${søknadKlarTilBehandling.tilstandsendring.tidspunkt.format(ISO_TIMESTAMP)}"
                },
                "utløstAv": "SØKNAD",
                "versjon": "dp:saksbehandling:1.2.3"
              }
            }
            """.trimIndent()
        testRapid.inspektør.message(1).toString() shouldEqualSpecifiedJsonIgnoringOrder
            """
            {
              "@event_name": "oppgave_til_statistikk_v5",
              "oppgave": {
                "oppgaveId": "${søknadAvbrutt.oppgaveId}",
                "mottatt": "${søknadAvbrutt.mottatt.format(ISO_TIMESTAMP)}",
                "sakId": "${søknadAvbrutt.sakId}",
                "behandlingId": "${søknadAvbrutt.behandlingId}",
                "personIdent": "12345612345",
                "saksbehandlerIdent": "AB123",
                "beslutterIdent": "B987",
                "tilstandsendring": {
                  "sekvensnummer": 2,
                  "tilstandsendringId": "${søknadAvbrutt.tilstandsendring.tilstandsendringId}",
                  "tilstand": "AVBRUTT_MANUELT",
                  "tidspunkt": "${søknadAvbrutt.tilstandsendring.tidspunkt.format(ISO_TIMESTAMP)}"
                },
                "utløstAv": "SØKNAD",
                "versjon": "dp:saksbehandling:1.2.3",
                "behandlingResultat": "AVBRUTT",
                "behandlingÅrsak": "BEHANDLES_I_ARENA",
                "fagsystem": "ARENA",
                "arenaSakId": "123"
              }
            }
            """.trimIndent()
        testRapid.inspektør.message(2).toString() shouldEqualSpecifiedJsonIgnoringOrder
            """
            {
              "@event_name": "oppgave_til_statistikk_v5",
              "oppgave": {
                "oppgaveId": "${innsendingFerdigBehandlet.oppgaveId}",
                "mottatt": "${innsendingFerdigBehandlet.mottatt.format(ISO_TIMESTAMP)}",
                "sakId": "${innsendingFerdigBehandlet.sakId}",
                "behandlingId": "${innsendingFerdigBehandlet.behandlingId}",
                "personIdent": "12345612345",
                "saksbehandlerIdent": "SB111",
                "tilstandsendring": {
                  "sekvensnummer": 3,
                  "tilstandsendringId": "${innsendingFerdigBehandlet.tilstandsendring.tilstandsendringId}",
                  "tilstand": "FERDIG_BEHANDLET",
                  "tidspunkt": "${innsendingFerdigBehandlet.tilstandsendring.tidspunkt.format(ISO_TIMESTAMP)}"
                },
                "utløstAv": "INNSENDING",
                "versjon": "dp:saksbehandling:1.2.3",
                "behandlingResultat": "RettTilDagpenger"
              }
            }
            """.trimIndent()
        testRapid.inspektør.message(3).toString() shouldEqualSpecifiedJsonIgnoringOrder
            """
            {
              "@event_name": "oppgave_til_statistikk_v5",
              "oppgave": {
                "oppgaveId": "${oppgavePåVent.oppgaveId}",
                "mottatt": "${oppgavePåVent.mottatt.format(ISO_TIMESTAMP)}",
                "sakId": "${oppgavePåVent.sakId}",
                "behandlingId": "${oppgavePåVent.behandlingId}",
                "personIdent": "12345612345",
                "tilstandsendring": {
                  "sekvensnummer": 4,
                  "tilstandsendringId": "${oppgavePåVent.tilstandsendring.tilstandsendringId}",
                  "tilstand": "PAA_VENT",
                  "tidspunkt": "${oppgavePåVent.tilstandsendring.tidspunkt.format(ISO_TIMESTAMP)}"
                },
                "utløstAv": "SØKNAD",
                "versjon": "dp:saksbehandling:1.2.3",
                "resultatBegrunnelse": "AVVENT_MELDEKORT"
              }
            }
            """.trimIndent()
    }

    @Test
    fun `Skal ikke publisere oppgavetilstandsendringer til statistikk hvis tidligere kjøring ikke er fullført`() {
        every { saksbehandlingsstatistikkRepository.tidligereTilstandsendringerErOverført() } returns false

        runBlocking {
            StatistikkJob(
                rapidsConnection = testRapid,
                saksbehandlingsstatistikkRepository = saksbehandlingsstatistikkRepository,
            ).executeJob()
        }
        assert(testRapid.inspektør.size == 0)
    }
}
