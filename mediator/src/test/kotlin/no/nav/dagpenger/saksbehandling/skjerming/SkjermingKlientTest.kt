package no.nav.dagpenger.saksbehandling.skjerming

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
import no.nav.dagpenger.saksbehandling.skjerming.SkjermingHttpKlient.Companion.lagSkjermingHttpKlient
import org.junit.jupiter.api.Test

class SkjermingKlientTest {
    private val testTokenProvider: () -> String = { "token" }
    private val baseUrl = "http://baseUrl"

    @Test
    fun `Skal returnere success og skjermet true`() {
        val mockEngine =
            MockEngine { _ ->
                respond("true", headers = headersOf("Content-Type", "application/json"))
            }
        val skjermingHttpKlient =
            SkjermingHttpKlient(
                skjermingApiUrl = baseUrl,
                tokenProvider = testTokenProvider,
                httpClient = lagSkjermingHttpKlient(mockEngine, PrometheusRegistry()),
            )
        val skjermingResultat: Result<Boolean> =
            runBlocking {
                skjermingHttpKlient.erSkjermetPerson("12345612345")
            }
        skjermingResultat shouldBe Result.success(true)
    }

    @Test
    fun `Skal sende riktige headers`() {
        var actualContentType: ContentType? = null

        val mockEngine =
            MockEngine { request ->
                actualContentType = request.body.contentType
                respond("true", headers = headersOf("Content-Type", "application/json"))
            }
        runBlocking {
            SkjermingHttpKlient(
                skjermingApiUrl = baseUrl,
                tokenProvider = testTokenProvider,
                httpClient = lagSkjermingHttpKlient(mockEngine, PrometheusRegistry()),
            ).erSkjermetPerson("12345612345")
        }

        actualContentType shouldBe ContentType.Application.Json
    }

    @Test
    fun `Skal returnere success og skjermet false`() {
        val mockEngine =
            MockEngine { _ ->
                respond("false", headers = headersOf("Content-Type", "application/json"))
            }
        val skjermingHttpKlient =
            SkjermingHttpKlient(
                skjermingApiUrl = baseUrl,
                tokenProvider = testTokenProvider,
                httpClient = lagSkjermingHttpKlient(mockEngine, PrometheusRegistry()),
            )
        val skjermingResultat: Result<Boolean> =
            runBlocking {
                skjermingHttpKlient.erSkjermetPerson("12345612345")
            }
        skjermingResultat shouldBe Result.success(false)
    }

    @Test
    fun `Skal returnere failure`() {
        val mockEngine =
            MockEngine { _ ->
                respondError(
                    status = HttpStatusCode.BadRequest,
                    content = "{}",
                    headers = headersOf("Content-Type", "application/json"),
                )
            }
        val skjermingHttpKlient =
            SkjermingHttpKlient(
                skjermingApiUrl = baseUrl,
                tokenProvider = testTokenProvider,
                httpClient = lagSkjermingHttpKlient(mockEngine, PrometheusRegistry()),
            )
        val skjermingResultat: Result<Boolean> =
            runBlocking {
                skjermingHttpKlient.erSkjermetPerson("12345612345")
            }
        skjermingResultat.isFailure shouldBe true
    }

    @Test
    fun `har http klient metrikker`() {
        val mockEngine =
            MockEngine { _ ->
                respond("false", headers = headersOf("Content-Type", "application/json"))
            }
        val collectorRegistry = PrometheusRegistry()
        val skjermingHttpKlient =
            SkjermingHttpKlient(
                skjermingApiUrl = baseUrl,
                tokenProvider = testTokenProvider,
                httpClient = lagSkjermingHttpKlient(mockEngine, collectorRegistry),
            )
        runBlocking {
            repeat(5) {
                skjermingHttpKlient.erSkjermetPerson("12345612345")
            }
        }

        collectorRegistry.getSnapShot<CounterSnapshot> {
            it == "dp_saksbehandling_skjerming_http_klient_status"
        }.let { counterSnapshot ->
            counterSnapshot.dataPoints.single { it.labels["status"] == "200" }.value shouldBe 5.0
        }

        shouldNotThrowAny {
            collectorRegistry.getSnapShot<HistogramSnapshot> { it == "dp_saksbehandling_skjerming_http_klient_duration" }
        }
    }
}
