package no.nav.dagpenger.saksbehandling.maskinell

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.UUIDv7
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

internal class BehandlingHttpKlientTest {
    private val testTokenProvider: (String, String) -> String = { _, _ ->
        "token"
    }
    private val baseUrl = "http://baseUrl"
    private val saksbehandlerToken = "saksbehandlerToken"

    @Test
    fun `Skal hente json fra behandling`() {
        val mockEngine =
            MockEngine { _ ->
                respond(behandlingJsonResponse, headers = headersOf("Content-Type", "application/json"))
            }
        val behandlingHttpKlient =
            BehandlingHttpKlient(
                behandlingUrl = baseUrl,
                behandlingScope = "scope",
                tokenProvider = testTokenProvider,
                engine = mockEngine,
            )
        val rawBehandlingMap = runBlocking {
            behandlingHttpKlient.hentBehandling(UUIDv7.ny(), saksbehandlerToken)
        }

        rawBehandlingMap["behandlingId"] shouldNotBe null
        rawBehandlingMap["opplysning"] shouldNotBe null
    }

    @Test
    fun `Skal godkjenne behandling`() {
        val testIdent = "12345678901"
        val behandlingId = UUIDv7.ny()
        val mockEngine = MockEngine { request ->
            request.url.toString() shouldBe "$baseUrl/behandling/$behandlingId/godkjenn"
            respond(content = "", status = HttpStatusCode.NoContent)
        }

        val behandlingHttpKlient =
            BehandlingHttpKlient(
                behandlingUrl = baseUrl,
                behandlingScope = "scope",
                tokenProvider = testTokenProvider,
                engine = mockEngine,
            )

        runBlocking {
            behandlingHttpKlient.godkjennBehandling(behandlingId, ident = testIdent, saksbehandlerToken)
        }
    }

    @Test
    fun `Skal avbryte behandling`() {
        val testIdent = "12345678901"
        val behandlingId = UUIDv7.ny()
        val mockEngine = MockEngine { request ->
            request.url.toString() shouldBe "$baseUrl/behandling/$behandlingId/avbryt"
            respond(content = "", status = HttpStatusCode.NoContent)
        }

        val behandlingHttpKlient =
            BehandlingHttpKlient(
                behandlingUrl = baseUrl,
                behandlingScope = "scope",
                tokenProvider = testTokenProvider,
                engine = mockEngine,
            )

        runBlocking {
            behandlingHttpKlient.avbrytBehandling(behandlingId, ident = testIdent, saksbehandlerToken)
        }
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
