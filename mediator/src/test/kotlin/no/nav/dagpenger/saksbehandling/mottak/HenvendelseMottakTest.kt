package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.assertions.json.shouldEqualSpecifiedJsonIgnoringOrder
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.KlageMediator
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.UtløstAvType.KLAGE
import no.nav.dagpenger.saksbehandling.lagOppgave
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class HenvendelseMottakTest {
    private val testIdentMedSak = "12345612345"
    private val testIdentUtenSak = "11111155555"
    private val sakId = UUIDv7.ny()
    private val klageOppgave = lagOppgave(utløstAvType = KLAGE)
    private val testRapid = TestRapid()
    private val sakMediatorMock =
        mockk<SakMediator>().also {
            coEvery { it.finnSisteSakId(testIdentMedSak) } returns sakId
            coEvery { it.finnSisteSakId(testIdentUtenSak) } returns null
        }
    private val klageMediatorMock =
        mockk<KlageMediator>().also {
            coEvery { it.opprettKlage(any()) } returns klageOppgave
        }

    init {
        HenvendelseMottak(testRapid, sakMediatorMock, klageMediatorMock)
    }

    @Test
    fun `Skal motta henvendelse om klage`() {
        val journalpostId = "1234"
        testRapid.sendTestMessage(
            key = testIdentMedSak,
            message =
                håndterHenvendelseBehov(
                    journalpostId = journalpostId,
                    kategori = "KLAGE",
                ),
        )
        testRapid.inspektør.size shouldBe 1

        testRapid.inspektør.message(0).toString() shouldEqualSpecifiedJsonIgnoringOrder
            """
            {
              "@event_name" : "behov",
              "@behov" : [ "HåndterHenvendelse" ],
              "journalpostId" : "$journalpostId",
              "fødselsnummer" : "$testIdentMedSak",
              "kategori" : "KLAGE",
              "@løsning" : {
                  "sakId" : "$sakId",
                  "håndtert" : true
              }
            }
            """.trimIndent()
    }

    private fun håndterHenvendelseBehov(
        journalpostId: String,
        kategori: String,
    ) = //language=JSON
        """
        {
          "@event_name" : "behov",
          "@behovId" : "${UUIDv7.ny()}",
          "@behov" : [ "HåndterHenvendelse" ],
          "journalpostId" : "$journalpostId",
          "fødselsnummer" : "$testIdentMedSak",
          "kategori" : "$kategori",
          "registrertDato" : "${LocalDateTime.now()}"
        }
        
        """.trimIndent()
}
