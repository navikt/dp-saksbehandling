package no.nav.dagpenger.behandling.iverksett

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.behandling.Meldingsfabrikk.testIdent
import no.nav.dagpenger.kontrakter.iverksett.IverksettDto
import org.junit.jupiter.api.Test
import java.util.UUID

class IverksettClientTest {
    private val testTokenProvider: (token: String, audience: String) -> String = { _, _ -> "testToken" }
    private val baseUrl = "http://baseUrl"
    val subjectToken = "gylidg_token"
    private val audience = "testAudience"

    val iverksettDto =
        IverksettDto(
            sakId = UUID.randomUUID(),
            behandlingId = UUID.randomUUID(),
            personIdent = testIdent,
        )

    @Test
    fun `Iverksett svarer med 202`() =
        runBlocking {
            val mockEngine = mockEngine(HttpStatusCode.Accepted)
            val iverksettClient =
                IverksettClient(baseUrl, audience, tokenProvider = testTokenProvider, engine = mockEngine)

            iverksettClient.iverksett(subjectToken, iverksettDto)
        }

    @Test
    fun `Det kastes en exception dersom kallet til iverksetting feiler`(): Unit =
        runBlocking {
            (399 until 599).forEach { statusCode ->
                val mockEngine = mockEngine(HttpStatusCode.fromValue(statusCode))
                val iverksettClient =
                    IverksettClient(baseUrl, audience, tokenProvider = testTokenProvider, engine = mockEngine)

                shouldThrow<RuntimeException> {
                    iverksettClient.iverksett(subjectToken, iverksettDto)
                }
            }
        }

    private fun mockEngine(statusCode: HttpStatusCode) =
        MockEngine { request ->
            request.headers[HttpHeaders.Accept] shouldBe "application/json"
            request.headers[HttpHeaders.Authorization] shouldBe "Bearer ${
                testTokenProvider.invoke(
                    subjectToken,
                    audience,
                )
            }"
            respond(content = "", statusCode)
        }
}
