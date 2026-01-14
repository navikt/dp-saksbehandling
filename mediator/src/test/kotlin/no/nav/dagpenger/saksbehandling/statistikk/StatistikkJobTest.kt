package no.nav.dagpenger.saksbehandling.statistikk

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.assertions.json.shouldEqualSpecifiedJson
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.OppgaveTilstandslogg
import no.nav.dagpenger.saksbehandling.TestHelper
import no.nav.dagpenger.saksbehandling.Tilstandsendring
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.UtløstAvType
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.dagpenger.saksbehandling.hendelser.ManuellBehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class StatistikkJobTest {
    private val sakId1 = UUID.randomUUID()
    private val sakId2 = UUID.randomUUID()

    private val avsluttetTidspunkt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)

    private val oppgave1 =
        TestHelper.lagOppgave(
            behandling = TestHelper.lagBehandling(utløstAvType = UtløstAvType.SØKNAD),
            tilstandslogg =
                OppgaveTilstandslogg(
                    Tilstandsendring(
                        hendelse = TomHendelse,
                        tilstand = Oppgave.Tilstand.Type.FERDIG_BEHANDLET,
                        tidspunkt = avsluttetTidspunkt,
                    ),
                ),
        )
    val basertPåBehandlingIdForOppgave2 = UUIDv7.ny()
    private val oppgave2 =
        TestHelper.lagOppgave(
            behandling =
                TestHelper.lagBehandling(
                    utløstAvType = UtløstAvType.MANUELL,
                    hendelse =
                        ManuellBehandlingOpprettetHendelse(
                            manuellId = UUIDv7.ny(),
                            behandlingId = UUIDv7.ny(),
                            ident = "MIKKE",
                            opprettet = TestHelper.opprettetNå,
                            basertPåBehandling = basertPåBehandlingIdForOppgave2,
                            behandlingskjedeId = basertPåBehandlingIdForOppgave2,
                        ),
                ),
            tilstandslogg =
                OppgaveTilstandslogg(
                    Tilstandsendring(
                        hendelse = TomHendelse,
                        tilstand = Oppgave.Tilstand.Type.AVBRUTT,
                        tidspunkt = avsluttetTidspunkt,
                    ),
                ),
        )

    private val testRapid = TestRapid()
    private val sakMediator =
        mockk<SakMediator>(relaxed = true).also {
            every { it.hentSakIdForBehandlingId(oppgave1.behandling.behandlingId) } returns sakId1
            every { it.hentSakIdForBehandlingId(oppgave2.behandling.behandlingId) } returns sakId2
        }
    private val statistikkTjeneste =
        mockk<StatistikkTjeneste>().also {
            every { it.oppgaverTilStatistikk() } returns listOf(oppgave1.oppgaveId, oppgave2.oppgaveId)
            every { it.markerOppgaveTilStatistikkSomOverført(oppgave1.oppgaveId) } returns 1
            every { it.markerOppgaveTilStatistikkSomOverført(oppgave2.oppgaveId) } returns 1
            every { it.tidligereOppgaverErOverførtTilStatistikk() } returns true
        }
    private val oppgaveRepository =
        mockk<OppgaveRepository>().also {
            every { it.hentOppgave(oppgave1.oppgaveId) } returns oppgave1
            every { it.hentOppgave(oppgave2.oppgaveId) } returns oppgave2
        }

    @Test
    fun `Skal publisere oppgaver til statistikk på riktig format og sette oppgaven som publisert`() {
        System.setProperty("NAIS_APP_IMAGE", "dp:saksbehandling:1.2.3")
        runBlocking {
            StatistikkJob(
                rapidsConnection = testRapid,
                sakMediator = sakMediator,
                statistikkTjeneste = statistikkTjeneste,
                oppgaveRepository = oppgaveRepository,
            ).executeJob()
        }

        verify(exactly = 1) {
            statistikkTjeneste.markerOppgaveTilStatistikkSomOverført(oppgave1.oppgaveId)
            statistikkTjeneste.markerOppgaveTilStatistikkSomOverført(oppgave2.oppgaveId)
        }

        testRapid.inspektør.message(0).toString() shouldEqualSpecifiedJson
            """
            {
                "@event_name": "oppgave_til_statistikk",
                "oppgave": {
                    "sakId": "$sakId1",
                    "oppgaveId": "${oppgave1.oppgaveId}",
                    "behandling": {
                        "behandlingId": "${oppgave1.behandling.behandlingId}",
                        "tidspunkt": "${oppgave1.behandling.opprettet}",
                        "basertPåBehandlingId": null,
                        "utløstAv": {
                            "type": "${oppgave1.behandling.utløstAv.name}",
                            "tidspunkt": "${oppgave1.behandling.opprettet}"
                        }
                    },
                    "personIdent": "${oppgave1.personIdent()}",
                    "oppgaveTilstander": [
                        {
                            "tilstand": "FERDIG_BEHANDLET",
                            "tidspunkt": "$avsluttetTidspunkt"
                        }

                    ],
                    "versjon": "dp:saksbehandling:1.2.3",
                    "avsluttetTidspunkt": "$avsluttetTidspunkt"
                }
            
            }
            """.trimIndent()
        testRapid.inspektør.message(1).toString() shouldEqualSpecifiedJson
            """
            {
                "@event_name": "oppgave_til_statistikk",
                "oppgave": {
                    "sakId": "$sakId2",
                    "oppgaveId": "${oppgave2.oppgaveId}",
                    "behandling": {
                        "behandlingId": "${oppgave2.behandling.behandlingId}",
                        "tidspunkt": "${oppgave2.behandling.opprettet}",
                        "basertPåBehandlingId": "$basertPåBehandlingIdForOppgave2",
                        "utløstAv": {
                            "type": "${oppgave2.behandling.utløstAv.name}",
                            "tidspunkt": "${oppgave2.behandling.opprettet}"
                        }
                    },
                    "personIdent": "${oppgave2.personIdent()}",
                    "saksbehandlerIdent": "${oppgave2.sisteSaksbehandler()}",
                    "beslutterIdent": "${oppgave2.sisteBeslutter()}",
                    "oppgaveTilstander": [
                        {
                            "tilstand": "AVBRUTT",
                            "tidspunkt": "$avsluttetTidspunkt"
                        }
                    ],
                    "versjon": "dp:saksbehandling:1.2.3",
                    "avsluttetTidspunkt": "$avsluttetTidspunkt"
                }
            
            }
            """.trimIndent()
    }

    @Test
    fun `Skal ikke publisere oppgaver til statistikk hvis det finnes tidligere oppgaver som ikke er overført`() {
        every { statistikkTjeneste.tidligereOppgaverErOverførtTilStatistikk() } returns false

        runBlocking {
            StatistikkJob(
                rapidsConnection = testRapid,
                sakMediator = sakMediator,
                statistikkTjeneste = statistikkTjeneste,
                oppgaveRepository = oppgaveRepository,
            ).executeJob()
        }

        assert(testRapid.inspektør.size == 0)
    }
}
