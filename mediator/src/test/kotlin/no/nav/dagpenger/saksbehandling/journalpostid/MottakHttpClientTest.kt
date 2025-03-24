package no.nav.dagpenger.saksbehandling.journalpostid

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondBadRequest
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.prometheus.metrics.model.registry.PrometheusRegistry
import io.prometheus.metrics.model.snapshots.CounterSnapshot
import io.prometheus.metrics.model.snapshots.HistogramSnapshot
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.getSnapShot
import org.junit.jupiter.api.Test
import java.util.UUID

class MottakHttpClientTest {
    private val søknadId = UUID.randomUUID()
    private val testIdent: String = "123123"

    @Test
    fun `Hent journalpostIder`() {
        var requestBody: String? = null
        val mockEngine =
            MockEngine { request ->
                request.headers[HttpHeaders.Authorization] shouldBe "Bearer token"
                request.url.host shouldBe "localhost"
                request.url.segments shouldBe listOf("v1", "journalpost", "sok")
                requestBody = request.body.toByteArray().toString(Charsets.UTF_8)
                respond(
                    """
                    {
                      "journalpostIder": ["1234", "5678"]
                    } 
                    """.trimIndent(),
                    headers = headersOf("Content-Type", "application/json"),
                )
            }

        val journalpostIdClient =
            MottakHttpKlient(
                dpMottakApiUrl = "http://localhost:8080",
                tokenProvider = { "token" },
                httpClient =
                    httpClient(
                        prometheusRegistry = PrometheusRegistry(),
                        engine = mockEngine,
                    ),
            )
        val journalpostIdResultat: Result<List<String>> =
            runBlocking {
                journalpostIdClient.hentJournalpostIder(søknadId = søknadId, testIdent)
            }
        journalpostIdResultat shouldBe Result.success(listOf("1234", "5678"))
    }

    @Test
    fun `Kall mot journalpostId api feiler`() {
        val mockEngine =
            MockEngine { _ ->
                respondBadRequest()
            }

        val journalpostIdClient =
            MottakHttpKlient(
                dpMottakApiUrl = "http://localhost:8080",
                tokenProvider = { "tøken" },
                httpClient =
                    httpClient(
                        prometheusRegistry = PrometheusRegistry(),
                        engine = mockEngine,
                    ),
            )
        val journalpostIdResultat =
            runBlocking {
                journalpostIdClient.hentJournalpostIder(søknadId = søknadId, ident = testIdent)
            }
        journalpostIdResultat.isFailure shouldBe true
    }

    @Test
    fun `Har metrikker`() {
        val mockEngine =
            MockEngine { _ ->
                respondBadRequest()
            }

        val collectorRegistry = PrometheusRegistry()
        val journalpostIdClient =
            MottakHttpKlient(
                dpMottakApiUrl = "http://localhost:8080",
                tokenProvider = { "tøken" },
                httpClient =
                    httpClient(
                        prometheusRegistry = collectorRegistry,
                        engine = mockEngine,
                    ),
            )
        runBlocking {
            repeat(5) {
                journalpostIdClient.hentJournalpostIder(søknadId = søknadId, ident = testIdent)
            }
        }

        collectorRegistry.getSnapShot<CounterSnapshot> {
            it == "dp_saksbehandling_dp_mottak_http_klient_status"
        }.let { counterSnapshot ->
            counterSnapshot.dataPoints.single { it.labels["status"] == "400" }.value shouldBe 5.0
        }

        shouldNotThrowAny {
            collectorRegistry.getSnapShot<HistogramSnapshot> { it == "dp_saksbehandling_dp_mottak_http_klient_duration" }
        }
    }
}
