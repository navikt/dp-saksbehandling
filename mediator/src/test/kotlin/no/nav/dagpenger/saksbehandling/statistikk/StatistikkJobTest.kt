package no.nav.dagpenger.saksbehandling.statistikk

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.assertions.json.shouldEqualSpecifiedJsonIgnoringOrder
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.AVBRUTT
import no.nav.dagpenger.saksbehandling.OppgaveTilstandslogg
import no.nav.dagpenger.saksbehandling.TestHelper
import no.nav.dagpenger.saksbehandling.TestHelper.opprettetNå
import no.nav.dagpenger.saksbehandling.Tilstandsendring
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.UtløstAvType
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.dagpenger.saksbehandling.hendelser.ManuellBehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.UUID

@Disabled("Midlertidig deaktivert")
class StatistikkJobTest {
    private val sakId1 = UUID.randomUUID()
    private val sakId2 = UUID.randomUUID()
    private val oppgaveFerdigBehandlet =
        TestHelper.lagOppgave(
            behandling = TestHelper.lagBehandling(utløstAvType = UtløstAvType.SØKNAD),
            tilstandslogg = TestHelper.lagOppgaveTilstandslogg(ferdigBehandlet = true),
        )
    val basertPåBehandlingId = UUIDv7.ny()
    val avbruttTidspunkt = opprettetNå.minusSeconds(22)
    private val oppgaveAvbrutt =
        TestHelper.lagOppgave(
            behandling =
                TestHelper.lagBehandling(
                    utløstAvType = UtløstAvType.MANUELL,
                    hendelse =
                        ManuellBehandlingOpprettetHendelse(
                            manuellId = UUIDv7.ny(),
                            behandlingId = UUIDv7.ny(),
                            ident = "MIKKE",
                            opprettet = opprettetNå,
                            basertPåBehandling = basertPåBehandlingId,
                            behandlingskjedeId = basertPåBehandlingId,
                        ),
                ),
            tilstandslogg =
                OppgaveTilstandslogg(
                    Tilstandsendring(
                        tilstand = AVBRUTT,
                        hendelse = TomHendelse,
                        tidspunkt = avbruttTidspunkt,
                    ),
                ),
        )

    private val testRapid = TestRapid()
    private val sakMediator =
        mockk<SakMediator>(relaxed = true).also {
            every { it.hentSakIdForBehandlingId(oppgaveFerdigBehandlet.behandling.behandlingId) } returns sakId1
            every { it.hentSakIdForBehandlingId(oppgaveAvbrutt.behandling.behandlingId) } returns sakId2
        }

//    private val statistikkTjeneste =
//        mockk<StatistikkTjeneste>().also {
//            every { it.oppgaverTilStatistikk() } returns listOf(oppgaveFerdigBehandlet.oppgaveId, oppgaveAvbrutt.oppgaveId)
//            every { it.markerOppgaveTilStatistikkSomOverført(oppgaveFerdigBehandlet.oppgaveId) } returns 1
//            every { it.markerOppgaveTilStatistikkSomOverført(oppgaveAvbrutt.oppgaveId) } returns 1
//            every { it.tidligereOppgaveTilstandsendringErOverfort() } returns true
//        }
    private val oppgaveRepository =
        mockk<OppgaveRepository>().also {
            every { it.hentOppgave(oppgaveFerdigBehandlet.oppgaveId) } returns oppgaveFerdigBehandlet
            every { it.hentOppgave(oppgaveAvbrutt.oppgaveId) } returns oppgaveAvbrutt
        }

    @Test
    fun `Skal publisere oppgaver til statistikk på riktig format og sette oppgaven som publisert`() {
//        System.setProperty("NAIS_APP_IMAGE", "dp:saksbehandling:1.2.3")
//        runBlocking {
//            StatistikkJob(
//                rapidsConnection = testRapid,
//                sakMediator = sakMediator,
//                statistikkTjeneste = statistikkTjeneste,
//                oppgaveRepository = oppgaveRepository,
//            ).executeJob()
//        }
//
//        verify(exactly = 1) {
//            statistikkTjeneste.markerOppgaveTilStatistikkSomOverført(oppgaveFerdigBehandlet.oppgaveId)
//            statistikkTjeneste.markerOppgaveTilStatistikkSomOverført(oppgaveAvbrutt.oppgaveId)
//        }
//
        testRapid.inspektør.message(0).toString() shouldEqualSpecifiedJsonIgnoringOrder
            """
            {
                "@event_name": "oppgave_til_statistikk",
                "oppgave": {
                    "sakId": "$sakId1",
                    "oppgaveId": "${oppgaveFerdigBehandlet.oppgaveId}",
                    "behandling": {
                        "behandlingId": "${oppgaveFerdigBehandlet.behandling.behandlingId}",
                        "tidspunkt": "${oppgaveFerdigBehandlet.behandling.opprettet}",
                        "basertPåBehandlingId": null,
                        "utløstAv": {
                            "type": "${oppgaveFerdigBehandlet.behandling.utløstAv.name}",
                            "tidspunkt": "${oppgaveFerdigBehandlet.behandling.opprettet}"
                        }
                    },
                    "personIdent": "${oppgaveFerdigBehandlet.personIdent()}",
                    "saksbehandlerIdent": "${oppgaveFerdigBehandlet.sisteSaksbehandler()}",
                    "beslutterIdent": "${oppgaveFerdigBehandlet.sisteBeslutter()}",
                    "oppgaveTilstander": [
                        {
                            "tilstand": "FERDIG_BEHANDLET",
                            "tidspunkt": "$opprettetNå"
                        },
                        {
                            "tilstand": "UNDER_KONTROLL",
                            "tidspunkt": "${opprettetNå.minusDays(1)}"
                        },
                        {
                            "tilstand": "UNDER_BEHANDLING",
                            "tidspunkt": "${opprettetNå.minusDays(2)}"
                        },
                        {
                            "tilstand": "KLAR_TIL_BEHANDLING",
                            "tidspunkt": "${opprettetNå.minusDays(3)}"
                        }
                    ],
                    "versjon": "dp:saksbehandling:1.2.3",
                    "avsluttetTidspunkt": "$opprettetNå"
                }
            
            }
            """.trimIndent()
        testRapid.inspektør.message(1).toString() shouldEqualSpecifiedJsonIgnoringOrder
            """
            {
                "@event_name": "oppgave_til_statistikk",
                "oppgave": {
                    "sakId": "$sakId2",
                    "oppgaveId": "${oppgaveAvbrutt.oppgaveId}",
                    "behandling": {
                        "behandlingId": "${oppgaveAvbrutt.behandling.behandlingId}",
                        "tidspunkt": "${oppgaveAvbrutt.behandling.opprettet}",
                        "basertPåBehandlingId": "$basertPåBehandlingId",
                        "utløstAv": {
                            "type": "${oppgaveAvbrutt.behandling.utløstAv.name}",
                            "tidspunkt": "${oppgaveAvbrutt.behandling.opprettet}"
                        }
                    },
                    "personIdent": "${oppgaveAvbrutt.personIdent()}",
                    "oppgaveTilstander": [
                        {
                            "tilstand": "AVBRUTT",
                            "tidspunkt": "$avbruttTidspunkt"
                        }
                    ],
                    "versjon": "dp:saksbehandling:1.2.3",
                    "avsluttetTidspunkt": "$avbruttTidspunkt"
                }
            
            }
            """.trimIndent()
    }

    @Test
    fun `Skal ikke publisere oppgaver til statistikk hvis det finnes tidligere oppgaver som ikke er overført`() {
//        every { statistikkTjeneste.tidligereOppgaveTilstandsendringErOverfort() } returns false
//
//        runBlocking {
//            StatistikkJob(
//                rapidsConnection = testRapid,
//                sakMediator = sakMediator,
//                statistikkTjeneste = statistikkTjeneste,
//                oppgaveRepository = oppgaveRepository,
//            ).executeJob()
//        }
//
//        assert(testRapid.inspektør.size == 0)
    }
}
