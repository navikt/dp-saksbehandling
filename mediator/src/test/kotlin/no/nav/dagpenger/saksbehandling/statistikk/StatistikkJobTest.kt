package no.nav.dagpenger.saksbehandling.statistikk

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.assertions.json.shouldEqualSpecifiedJson
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.TestHelper
import no.nav.dagpenger.saksbehandling.TestHelper.lagTilstandLogg
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import org.junit.jupiter.api.Test
import java.util.UUID

class StatistikkJobTest {
    private val sakId1 = UUID.randomUUID()
    private val sakId2 = UUID.randomUUID()
    private val oppgave1 = TestHelper.lagOppgave()
    private val oppgave2 = TestHelper.lagOppgave(tilstandslogg = lagTilstandLogg())

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
                    "oppgaveTilstander": [],
                    "versjon": "dp:saksbehandling:1.2.3"
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
                        "basertPåBehandlingId": null,
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
                            "tilstand": "${oppgave2.tilstandslogg[0].tilstand.name}",
                            "tidspunkt": "${oppgave2.tilstandslogg[0].tidspunkt}"
                        },
                        {
                            "tilstand": "${oppgave2.tilstandslogg[1].tilstand.name}",
                            "tidspunkt": "${oppgave2.tilstandslogg[1].tidspunkt}"
                        },
                        {
                            "tilstand": "${oppgave2.tilstandslogg[2].tilstand.name}",
                            "tidspunkt": "${oppgave2.tilstandslogg[2].tidspunkt}"
                        }
                    ],
                    "versjon": "dp:saksbehandling:1.2.3"
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
