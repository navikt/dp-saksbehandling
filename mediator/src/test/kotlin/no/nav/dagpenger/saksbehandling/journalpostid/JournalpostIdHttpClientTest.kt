package no.nav.dagpenger.saksbehandling.journalpostid

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondBadRequest
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.prometheus.client.CollectorRegistry
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.UUID

class JournalpostIdHttpClientTest {
    val søknadId = UUID.randomUUID()

    @Test
    fun `Hent journalpostId`() {
        val mockEngine =
            MockEngine { request ->
                request.headers[HttpHeaders.Authorization] shouldBe "Bearer tøken"
                request.url.host shouldBe "localhost"
                request.url.pathSegments shouldContain (søknadId.toString())
                respond("1234", headers = headersOf("Content-Type", "plain/text"))
            }

        val journalpostIdClient =
            JournalpostIdHttpClient(
                journalpostIdApiUrl = "http://localhost:8080",
                tokenProvider = { "tøken" },
                httpClient =
                    httpClient(
                        collectorRegistry = CollectorRegistry(),
                        engine = mockEngine,
                    ),
            )
        val journalpostIdResultat: Result<String> =
            runBlocking {
                journalpostIdClient.hentJournalpostId(søknadId = søknadId)
            }
        journalpostIdResultat shouldBe Result.success("1234")
    }

    @Test
    fun `Kall mot journalpostId api feiler`() {
        val mockEngine =
            MockEngine { _ ->
                respondBadRequest()
            }

        val journalpostIdClient =
            JournalpostIdHttpClient(
                journalpostIdApiUrl = "http://localhost:8080/$søknadId",
                tokenProvider = { "tøken" },
                httpClient =
                    httpClient(
                        collectorRegistry = CollectorRegistry(),
                        engine = mockEngine,
                    ),
            )
        val journalpostIdResultat: Result<String> =
            runBlocking {
                journalpostIdClient.hentJournalpostId(søknadId = søknadId)
            }
        journalpostIdResultat.isFailure shouldBe true
    }

    @Test
    fun `Har metrikker`() {
        val mockEngine =
            MockEngine { _ ->
                respondBadRequest()
            }

        val collectorRegistry = CollectorRegistry()
        val journalpostIdClient =
            JournalpostIdHttpClient(
                journalpostIdApiUrl = "http://localhost:8080/$søknadId",
                tokenProvider = { "tøken" },
                httpClient =
                    httpClient(
                        collectorRegistry = collectorRegistry,
                        engine = mockEngine,
                    ),
            )
        runBlocking {
            repeat(5) {
                journalpostIdClient.hentJournalpostId(søknadId = søknadId)
            }
        }

        collectorRegistry.getSampleValue(
            "dp_saksbehandling_joark_http_klient_status_total",
            listOf("status").toTypedArray(),
            listOf("400").toTypedArray(),
        ) shouldBe 5.0
    }
}
