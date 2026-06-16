package no.nav.dagpenger.saksbehandling.meldekortregister

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.prometheus.metrics.model.registry.PrometheusRegistry
import io.prometheus.metrics.model.snapshots.CounterSnapshot
import io.prometheus.metrics.model.snapshots.HistogramSnapshot
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.getSnapShot
import org.junit.jupiter.api.Test
import java.util.UUID

class MeldekortregisterKlientTest {
    private val testTokenProvider: () -> String = { "token" }
    private val baseUrl = "http://meldekortregister"
    private val testIdent = "12345678901"
    private val testSøknadId = UUID.randomUUID()

    @Test
    fun `Skal returnere success med true når tjenesten svarer at bruker har avvikende meldesyklus`() {
        val mockEngine =
            MockEngine { _ ->
                respond(
                    """{"harMeldekortMedEndretMeldesyklus":true}""",
                    headers = headersOf("Content-Type", "application/json"),
                )
            }
        val klient = MeldekortregisterKlient(baseUrl, testTokenProvider, lagHttpKlient(mockEngine))

        val resultat = runBlocking { klient.harMeldekortMedEndretMeldesyklus(testIdent, testSøknadId) }

        resultat shouldBe Result.success(true)
    }

    @Test
    fun `Skal returnere success med false når tjenesten svarer at bruker ikke har avvikende meldesyklus`() {
        val mockEngine =
            MockEngine { _ ->
                respond(
                    """{"harMeldekortMedEndretMeldesyklus":false}""",
                    headers = headersOf("Content-Type", "application/json"),
                )
            }
        val klient = MeldekortregisterKlient(baseUrl, testTokenProvider, lagHttpKlient(mockEngine))

        val resultat = runBlocking { klient.harMeldekortMedEndretMeldesyklus(testIdent, testSøknadId) }

        resultat shouldBe Result.success(false)
    }

    @Test
    fun `Skal returnere success med false når tjenesten svarer 404`() {
        val mockEngine =
            MockEngine { _ ->
                respondError(
                    status = HttpStatusCode.NotFound,
                    content = "",
                    headers = headersOf("Content-Type", "application/json"),
                )
            }
        val klient = MeldekortregisterKlient(baseUrl, testTokenProvider, lagHttpKlient(mockEngine))

        val resultat = runBlocking { klient.harMeldekortMedEndretMeldesyklus(testIdent, testSøknadId) }

        resultat shouldBe Result.success(false)
    }

    @Test
    fun `Skal returnere failure ved uventet HTTP-feil`() {
        val mockEngine =
            MockEngine { _ ->
                respondError(
                    status = HttpStatusCode.InternalServerError,
                    content = "{}",
                    headers = headersOf("Content-Type", "application/json"),
                )
            }
        val klient = MeldekortregisterKlient(baseUrl, testTokenProvider, lagHttpKlient(mockEngine))

        val resultat = runBlocking { klient.harMeldekortMedEndretMeldesyklus(testIdent, testSøknadId) }

        resultat.isFailure shouldBe true
    }

    @Test
    fun `Skal sende Authorization-header`() {
        var actualAuthHeader: String? = null
        val mockEngine =
            MockEngine { request ->
                actualAuthHeader = request.headers["Authorization"]
                respond(
                    """{"harMeldekortMedEndretMeldesyklus":false}""",
                    headers = headersOf("Content-Type", "application/json"),
                )
            }
        val klient = MeldekortregisterKlient(baseUrl, testTokenProvider, lagHttpKlient(mockEngine))

        runBlocking { klient.harMeldekortMedEndretMeldesyklus(testIdent, testSøknadId) }

        actualAuthHeader shouldBe "Bearer token"
    }

    @Test
    fun `Skal sende Content-Type application-json`() {
        var actualContentType: ContentType? = null
        val mockEngine =
            MockEngine { request ->
                actualContentType = request.body.contentType
                respond(
                    """{"harMeldekortMedEndretMeldesyklus":false}""",
                    headers = headersOf("Content-Type", "application/json"),
                )
            }
        val klient = MeldekortregisterKlient(baseUrl, testTokenProvider, lagHttpKlient(mockEngine))

        runBlocking { klient.harMeldekortMedEndretMeldesyklus(testIdent, testSøknadId) }

        actualContentType shouldBe ContentType.Application.Json
    }

    @Test
    fun `Har http klient metrikker`() {
        val mockEngine =
            MockEngine { _ ->
                respond(
                    """{"harMeldekortMedEndretMeldesyklus":false}""",
                    headers = headersOf("Content-Type", "application/json"),
                )
            }
        val collectorRegistry = PrometheusRegistry()
        val klient = MeldekortregisterKlient(baseUrl, testTokenProvider, lagHttpKlient(mockEngine, collectorRegistry))

        runBlocking {
            repeat(3) { klient.harMeldekortMedEndretMeldesyklus(testIdent, testSøknadId) }
        }

        collectorRegistry
            .getSnapShot<CounterSnapshot> {
                it == "dp_saksbehandling_dp_meldekortregister_http_klient_status"
            }.let { counterSnapshot ->
                counterSnapshot.dataPoints.single { it.labels["status"] == "200" }.value shouldBe 3.0
            }

        shouldNotThrowAny {
            collectorRegistry.getSnapShot<HistogramSnapshot> {
                it == "dp_saksbehandling_dp_meldekortregister_http_klient_duration"
            }
        }
    }

    private fun lagHttpKlient(
        engine: MockEngine,
        prometheusRegistry: PrometheusRegistry = PrometheusRegistry(),
    ) = lagMeldekortregisterHttpKlient(prometheusRegistry, engine)
}
