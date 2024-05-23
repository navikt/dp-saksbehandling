package no.nav.dagpenger.saksbehandling.journalpostid

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondBadRequest
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
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
                request.url.pathSegments shouldContain (søknadId.toString())
                respond("1234", headers = headersOf("Content-Type", "plain/text"))
            }

        val journalpostIdClient =
            JournalpostIdHttpClient(
                journalpostIdApiUrl = "http://localhost:8080/$søknadId",
                tokenProvider = { "tøken" },
                httpClient = httpClient(mockEngine),
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
                httpClient = httpClient(mockEngine),
            )
        val journalpostIdResultat: Result<String> =
            runBlocking {
                journalpostIdClient.hentJournalpostId(søknadId = søknadId)
            }
        journalpostIdResultat.isFailure shouldBe true
    }
}
