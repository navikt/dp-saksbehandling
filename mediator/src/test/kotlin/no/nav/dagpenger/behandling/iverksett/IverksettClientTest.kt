package no.nav.dagpenger.behandling.iverksett

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
            val iverksettClient = IverksettClient(baseUrl, audience, tokenProvider = testTokenProvider, engine = mockEngine)

            iverksettClient.iverksett(subjectToken, iverksettDto)
        }

    // TODO: Testcases for de ulike feilscenarier
//    @Test
//    fun `Iverksett svarer med 400`(): Unit = runBlocking {
//        val mockEngine = mockEngine(HttpStatusCode.Forbidden)
//        val iverksettClient = IverksettClient(baseUrl, audience, tokenProvider = testTokenProvider, engine = mockEngine)
//
//    }

    private fun mockEngine(statusCode: HttpStatusCode) =
        MockEngine { request ->
            request.headers[HttpHeaders.Accept] shouldBe "application/json"
            request.headers[HttpHeaders.Authorization] shouldBe "Bearer ${testTokenProvider.invoke(subjectToken, audience)}"
            respond(content = "", statusCode)
        }
}
