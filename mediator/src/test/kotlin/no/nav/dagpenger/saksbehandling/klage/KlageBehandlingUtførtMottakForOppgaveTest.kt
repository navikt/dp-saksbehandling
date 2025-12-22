package no.nav.dagpenger.saksbehandling.klage

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.TilgangType
import org.junit.jupiter.api.Test
import java.util.UUID

class KlageBehandlingUtførtMottakForOppgaveTest {
    private val testRapid = TestRapid()

    @Test
    fun `Skal håndtere klage_behandling_utført hendelse`() {
        val testOppgaveId = UUID.randomUUID()
        val testBehandlingId = UUID.randomUUID()
        val testUtfall = UtfallType.OPPRETTHOLDELSE
        val testIdent = "12345678901"
        val testSaksbehandler =
            Saksbehandler(
                navIdent = "Z123456",
                grupper = emptySet(),
                tilganger =
                    setOf(
                        TilgangType.BESLUTTER,
                        TilgangType.SAKSBEHANDLER,
                    ),
            )
        val mockOppgaveMediator =
            mockk<OppgaveMediator>(relaxed = false).also {
                every { it.ferdigstillOppgave(testBehandlingId, testSaksbehandler) } returns
                    Result.success(
                        testOppgaveId,
                    )
            }

        KlageBehandlingUtførtMottakForOppgave(testRapid, mockOppgaveMediator)

        val klageBehandlingUtførtJson =
            // lang=JSON
            """
            {
                "@event_name": "klage_behandling_utført",
                "behandlingId": "$testBehandlingId",
                "utfall": "${testUtfall.name}",
                "ident": "$testIdent",
                "saksbehandler":  {
                    "navIdent": "${testSaksbehandler.navIdent}",
                    "grupper": [],
                    "tilganger": ["BESLUTTER", "SAKSBEHANDLER"]
                }
                
            }
            """.trimIndent()

        testRapid.sendTestMessage(klageBehandlingUtførtJson)
        verify(exactly = 1) { mockOppgaveMediator.ferdigstillOppgave(testBehandlingId, testSaksbehandler) }
    }
}
