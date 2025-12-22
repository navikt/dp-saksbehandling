package no.nav.dagpenger.saksbehandling.klage

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.TilgangType
import org.junit.jupiter.api.Test
import java.util.UUID

class AbstractKlageBehandlingUtførtMottakTest {
    private val testRapid = TestRapid()

    private class TestClass(
        rapidsConnection: RapidsConnection,
    ) : AbstractKlageBehandlingUtførtMottak(rapidsConnection) {
        var behandlingId: UUID? = null
        var utfall: UtfallType? = null
        var ident: String? = null
        var saksbehandler: Saksbehandler? = null
        override val mottakNavn: String = "TestKlageBehandlingUtførtMottak"

        override fun håndter(
            behandlingId: UUID,
            utfall: UtfallType,
            ident: String,
            saksbehandler: Saksbehandler,
        ) {
            this.behandlingId = behandlingId
            this.utfall = utfall
            this.ident = ident
            this.saksbehandler = saksbehandler
        }
    }

    @Test
    fun `Skal motta klage_behandling_utført hendelse`() {
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

        val testMottak = TestClass(testRapid)

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

        testMottak.behandlingId shouldBe testBehandlingId
        testMottak.utfall shouldBe testUtfall
        testMottak.ident shouldBe testIdent
        testMottak.saksbehandler shouldBe testSaksbehandler
    }
}
