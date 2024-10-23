package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.assertions.json.shouldEqualSpecifiedJsonIgnoringOrder
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator
import org.junit.jupiter.api.Test
import java.util.UUID

class MeldingOmVedtakProdusentBehovløserTest {
    private val rapid = TestRapid()

    @Test
    fun `skal løse behov dersom filter matcher`() {
        val behandlingMedUtsending = UUIDv7.ny()
        val behandlingUtenUtsending = UUIDv7.ny()

        val utsendingMediator =
            mockk<UtsendingMediator>().also {
                coEvery { it.utsendingFinnesForBehandling(behandlingMedUtsending) } returns true
                coEvery { it.utsendingFinnesForBehandling(behandlingUtenUtsending) } returns false
            }

        MeldingOmVedtakProdusentBehovløser(rapid, utsendingMediator)
        rapid.sendBehov(behandlingMedUtsending)
        rapid.sendBehov(behandlingUtenUtsending)

        rapid.inspektør.size shouldBe 2

        rapid.inspektør.message(0).toString() shouldEqualSpecifiedJsonIgnoringOrder
            """
            {
              "@event_name" : "behov",
              "@behov" : [ "MeldingOmVedtakProdusent" ],
              "behandlingId" : "$behandlingMedUtsending",
              "ident" : "11109233444",
              "@løsning" : {
                  "MeldingOmVedtakProdusent" : "Dagpenger"
              }
            }
            """.trimIndent()

        rapid.inspektør.message(1).toString() shouldEqualSpecifiedJsonIgnoringOrder
            """
            {
              "@event_name" : "behov",
              "@behov" : [ "MeldingOmVedtakProdusent" ],
              "behandlingId" : "$behandlingUtenUtsending",
              "ident" : "11109233444",
              "@løsning" : {
                  "MeldingOmVedtakProdusent" : "Arena"
              }
            }
            """.trimIndent()
    }

    private fun TestRapid.sendBehov(behandlingId: UUID) {
        this.sendTestMessage(
            """
            {
              "@event_name" : "behov",
              "@behov" : [ "MeldingOmVedtakProdusent" ],
              "behandlingId" : "$behandlingId",
              "ident" : "11109233444"
            }
            
            """.trimIndent(),
        )
    }
}
