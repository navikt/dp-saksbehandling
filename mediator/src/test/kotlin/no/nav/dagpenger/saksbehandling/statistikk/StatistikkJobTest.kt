package no.nav.dagpenger.saksbehandling.statistikk

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.assertions.json.shouldEqualSpecifiedJson
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.TestHelper
import no.nav.dagpenger.saksbehandling.TestHelper.lagTilstandLogg
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class StatistikkJobTest {
    private val sakId1 = UUID.randomUUID()
    private val sakId2 = UUID.randomUUID()
    private val oppgave1 = TestHelper.lagOppgave()
    private val oppgave2 = TestHelper.lagOppgave(tilstandslogg = lagTilstandLogg())
    private val now = LocalDateTime.now()

    private val testRapid = TestRapid()
    private val sakMediator =
        mockk<SakMediator>(relaxed = true).also {
            every { it.hentSakIdForBehandlingId(oppgave1.behandling.behandlingId) } returns sakId1
            every { it.hentSakIdForBehandlingId(oppgave2.behandling.behandlingId) } returns sakId2
        }
    private val statistikkTjeneste =
        mockk<StatistikkTjeneste>().also {
            every { it.oppgaverTilStatistikk() } returns
                listOf(
                    Pair(oppgave1.oppgaveId, now),
                    Pair(oppgave2.oppgaveId, now),
                )
            every {
                it.markerSomOverført(oppgave1.oppgaveId)
                it.markerSomOverført(oppgave2.oppgaveId)
            } returns 1
        }
    private val oppgaveRepository =
        mockk<OppgaveRepository>().also {
            every { it.hentOppgave(oppgave1.oppgaveId) } returns oppgave1
            every { it.hentOppgave(oppgave2.oppgaveId) } returns oppgave2
        }

    @Test
    fun `Skal publisere oppgaver til statistikk på riktig format`() {
        runBlocking {
            StatistikkJob(
                rapidsConnection = testRapid,
                sakMediator = sakMediator,
                statistikkTjeneste = statistikkTjeneste,
                oppgaveRepository = oppgaveRepository,
            ).executeJob()
        }

        testRapid.inspektør.message(0).toString() shouldEqualSpecifiedJson
            """
            {
                "@event_name": "statistikk_oppgave_ferdigstilt",
                "oppgave": {
                    "sakId": "$sakId1",
                    "behandling": {
                    "id": "${oppgave1.behandling.behandlingId}",
                    "tidspunkt": "${oppgave1.behandling.opprettet}",
                    "basertPåBehandling": null,
                    "utløstAv": {
                        "type": "${oppgave1.behandling.utløstAv.name}",
                        "tidspunkt": "${oppgave1.behandling.opprettet}"
                    }
                    },
                    "oppgaveTilstander": [],
                    "personIdent": "${oppgave1.personIdent()}"
                }
            
            }
            """.trimIndent()
        testRapid.inspektør.message(1).toString() shouldEqualSpecifiedJson
            """
            {
                "@event_name": "statistikk_oppgave_ferdigstilt",
                "oppgave": {
                    "sakId": "$sakId2",
                    "behandling": {
                    "id": "${oppgave2.behandling.behandlingId}",
                    "tidspunkt": "${oppgave2.behandling.opprettet}",
                    "basertPåBehandling": null,
                    "utløstAv": {
                        "type": "${oppgave2.behandling.utløstAv.name}",
                        "tidspunkt": "${oppgave2.behandling.opprettet}"
                    }
                    },
                    "oppgaveTilstander": [
                    {
                        "tilstand": "${oppgave2.tilstandslogg[0].tilstand.name}",
                        "tidspunkt": "${oppgave2.tilstandslogg[0].tidspunkt}",
                        "saksbehandlerIdent": null,
                        "beslutterIdent": null
                    },
                    {
                        "tilstand": "${oppgave2.tilstandslogg[1].tilstand.name}",
                        "tidspunkt": "${oppgave2.tilstandslogg[1].tidspunkt}",
                        "saksbehandlerIdent": null,
                        "beslutterIdent": null
                    },
                    {
                        "tilstand": "${oppgave2.tilstandslogg[2].tilstand.name}",
                        "tidspunkt": "${oppgave2.tilstandslogg[2].tidspunkt}",
                        "saksbehandlerIdent": null,
                        "beslutterIdent": null
                    }
                    ],
                    "personIdent": "${oppgave2.personIdent()}"
                }
            
            }
            """.trimIndent()
    }
}
