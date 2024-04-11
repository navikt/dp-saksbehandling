package no.nav.dagpenger.saksbehandling.skjerming

import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.createHttpClient
import org.junit.jupiter.api.Test

class SkjermingKlientTest {
    private val testTokenProvider: () -> String = { "token" }
    private val baseUrl = "http://baseUrl"

    @Test
    fun `Skal returnere success og skjermet true`() {
        val mockEngine =
            MockEngine { request ->
                respond("true", headers = headersOf("Content-Type", "application/json"))
            }
        val skjermingHttpKlient =
            SkjermingHttpKlient(
                skjermingApiUrl = baseUrl,
                tokenProvider = testTokenProvider,
                httpClient = createHttpClient(engine = mockEngine),
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
                httpClient = createHttpClient(engine = mockEngine),
            ).erSkjermetPerson("12345612345")
        }

        actualContentType shouldBe ContentType.Application.Json
    }

    @Test
    fun `Skal returnere success og skjermet false`() {
        val mockEngine =
            MockEngine { request ->
                respond("false", headers = headersOf("Content-Type", "application/json"))
            }
        val skjermingHttpKlient =
            SkjermingHttpKlient(
                skjermingApiUrl = baseUrl,
                tokenProvider = testTokenProvider,
                httpClient = createHttpClient(engine = mockEngine),
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
            MockEngine { request ->
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
                httpClient = createHttpClient(engine = mockEngine),
            )
        val skjermingResultat: Result<Boolean> =
            runBlocking {
                skjermingHttpKlient.erSkjermetPerson("12345612345")
            }
        skjermingResultat.isFailure shouldBe true
    }
}
