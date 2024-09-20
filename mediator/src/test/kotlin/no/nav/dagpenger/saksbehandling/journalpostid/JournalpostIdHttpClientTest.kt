package no.nav.dagpenger.saksbehandling.journalpostid

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondBadRequest
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.prometheus.metrics.model.registry.PrometheusRegistry
import io.prometheus.metrics.model.snapshots.CounterSnapshot
import io.prometheus.metrics.model.snapshots.HistogramSnapshot
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.getSnapShot
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
                        prometheusRegistry = PrometheusRegistry(),
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
                        prometheusRegistry = PrometheusRegistry(),
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

        val collectorRegistry = PrometheusRegistry()
        val journalpostIdClient =
            JournalpostIdHttpClient(
                journalpostIdApiUrl = "http://localhost:8080/$søknadId",
                tokenProvider = { "tøken" },
                httpClient =
                    httpClient(
                        prometheusRegistry = collectorRegistry,
                        engine = mockEngine,
                    ),
            )
        runBlocking {
            repeat(5) {
                journalpostIdClient.hentJournalpostId(søknadId = søknadId)
            }
        }

        collectorRegistry.getSnapShot<CounterSnapshot> {
            it == "dp_saksbehandling_joark_http_klient_status"
        }.let { counterSnapshot ->
            counterSnapshot.dataPoints.single { it.labels["status"] == "400" }.value shouldBe 5.0
        }

        shouldNotThrowAny {
            collectorRegistry.getSnapShot<HistogramSnapshot> { it == "dp_saksbehandling_joark_http_klient_duration" }
        }
    }
}
