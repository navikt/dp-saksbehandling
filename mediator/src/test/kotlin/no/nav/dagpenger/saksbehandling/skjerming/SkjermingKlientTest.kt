package no.nav.dagpenger.saksbehandling.skjerming

import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.createHttpClient
import org.junit.jupiter.api.Test

class SkjermingKlientTest {
    private val testTokenProvider: () -> String = { -> "token" }
    private val baseUrl = "http://baseUrl"

    @Test
    fun `Skal returnere 200 OK`() {
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
                skjermingHttpKlient.egenAnsatt("12345612345")
            }
        skjermingResultat shouldBe Result.success(true)
    }
}
