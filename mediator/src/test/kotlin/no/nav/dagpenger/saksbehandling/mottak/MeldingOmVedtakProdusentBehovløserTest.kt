package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.assertions.json.shouldEqualSpecifiedJsonIgnoringOrder
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator
import org.junit.jupiter.api.Test

class MeldingOmVedtakProdusentBehovløserTest {
    private val rapid = TestRapid()

    @Test
    fun `skal løse behov`() {
        val behandlingId = UUIDv7.ny()
        val behandlingIdString = behandlingId.toString()

        val utsendingMediator =
            mockk<UtsendingMediator>().also {
                coEvery { it.utsendingFinnesForBehandling(behandlingId) } returns true
            }

        MeldingOmVedtakProdusentBehovløser(rapid, utsendingMediator)
        rapid.sendTestMessage(
            """
            {
              "@event_name" : "behov",
              "@behov" : [ "MeldingOmVedtakProdusent" ],
              "behandlingId" : "$behandlingIdString",
              "ident" : "11109233444"
            }
            
            """.trimIndent(),
        )

        rapid.inspektør.size shouldBe 1
        rapid.inspektør.message(0).toString() shouldEqualSpecifiedJsonIgnoringOrder
            """
            {
                      "@event_name" : "behov",
                      "@behov" : [ "MeldingOmVedtakProdusent" ],
                      "behandlingId" : "$behandlingIdString",
                      "ident" : "11109233444",
                      "@løsning" : {
                          "MeldingOmVedtakProdusent" : "Dagpenger"
                      }
                    }
            """.trimIndent()
        verify(exactly = 1) {
            utsendingMediator.utsendingFinnesForBehandling(behandlingId)
        }
    }
}
