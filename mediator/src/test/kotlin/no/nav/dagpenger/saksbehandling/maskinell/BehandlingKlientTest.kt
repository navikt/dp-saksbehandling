package no.nav.dagpenger.saksbehandling.maskinell

import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldNotBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.UUIDv7
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

class BehandlingKlientTest {
    private val testTokenProvider: (String) -> String = { "testToken" }
    private val baseUrl = "http://baseUrl"
    private val saksbehandlerToken = "saksbehandlerToken"

    @Test
    fun `Skal hente json fra behandling`() {
        val mockEngine =
            MockEngine { request ->
                respond(behandlingJsonResponse, headers = headersOf("Content-Type", "application/json"))
            }
        val behandlingKlient =
            BehandlingKlient(
                behandlingUrl = baseUrl,
                oboTokenProvider = testTokenProvider,
                engine = mockEngine,
            )
        val behandling =
            runBlocking {
                behandlingKlient.hentBehandling(UUIDv7.ny(), saksbehandlerToken)
            }
        behandling shouldNotBe null
        behandling.opplysning.shouldNotBeEmpty()
        behandling.behandlingId shouldNotBe null
    }

    // language=json
    private val behandlingJsonResponse =
        """
        {
          "behandlingId": "018dc0e6-0be3-7f17-b410-08f2072ffcb1",
          "opplysning": [
            {
              "id": "018dc0e6-0d4b-7f11-a881-1377d9b38a2a",
              "opplysningstype": "Søknadstidspunkt",
              "verdi": "2024-02-19",
              "status": "Hypotese",
              "gyldigFraOgMed": "${OffsetDateTime.MIN}",
              "gyldigTilOgMed": "${OffsetDateTime.MAX}",
              "datatype": "LocalDate",
              "kilde": null,
              "utledetAv": null
            }
          ]
        }
                  
        """.trimIndent()
}
