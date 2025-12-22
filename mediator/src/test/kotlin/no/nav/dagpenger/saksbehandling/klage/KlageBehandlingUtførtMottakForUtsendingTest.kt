package no.nav.dagpenger.saksbehandling.klage

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.TilgangType
import no.nav.dagpenger.saksbehandling.UtsendingSak
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator
import no.nav.dagpenger.saksbehandling.utsending.hendelser.StartUtsendingHendelse
import org.junit.jupiter.api.Test
import java.util.UUID

class KlageBehandlingUtførtMottakForUtsendingTest {
    private val testRapid = TestRapid()

    @Test
    fun `Skal håndtere klage_behandling_utført hendelse`() {
        val testBehandlingId = UUID.randomUUID()
        val testSakId = UUID.randomUUID()
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
        val startUtsendingHendelse =
            StartUtsendingHendelse(
                behandlingId = testBehandlingId,
                utsendingSak =
                    UtsendingSak(
                        id = testSakId.toString(),
                        kontekst = "Dagpenger",
                    ),
                ident = testIdent,
            )
        val mockUtsendingMediator =
            mockk<UtsendingMediator>(relaxed = false).also {
                every { it.mottaStartUtsending(startUtsendingHendelse) } just Runs
            }

        KlageBehandlingUtførtMottakForUtsending(testRapid, mockUtsendingMediator)

        val klageBehandlingUtførtJson =
            // lang=JSON
            """
            {
                "@event_name": "klage_behandling_utført",
                "behandlingId": "$testBehandlingId",
                "sakId": "$testSakId",
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
        verify(exactly = 1) { mockUtsendingMediator.mottaStartUtsending(startUtsendingHendelse) }
    }
}
