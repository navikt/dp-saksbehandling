package no.nav.dagpenger.saksbehandling.pdl

import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondBadRequest
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.pdl.PDLPerson
import no.nav.dagpenger.saksbehandling.helper.fileAsText
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class PDLHttpKlientTest {
    @Test
    fun `Skal hente persondata fra PDL`() {
        val mockEngine =
            MockEngine { request ->
                respond(
                    pdlResponse("UGRADERT"),
                    headers = headersOf("Content-Type", "application/json"),
                )
            }

        val person =
            runBlocking {
                PDLHttpKlient(
                    url = "http://localhost:8080",
                    tokenSupplier = { "token" },
                    httpClient =
                        defaultHttpClient(
                            mockEngine,
                        ),
                ).person("12345612345").getOrThrow()
            }
        person.fornavn shouldBe "ÅPENHJERTIG"
        person.etternavn shouldBe "GYNGEHEST"
        person.mellomnavn shouldBe null
        person.kjønn shouldBe PDLPerson.Kjonn.KVINNE
        person.fødselsdato shouldBe LocalDate.of(1984, 3, 1)
        person.statsborgerskap shouldBe "NOR"
        person.alder shouldBe ChronoUnit.YEARS.between(person.fødselsdato, LocalDate.now())
    }

    @ParameterizedTest
    @CsvSource(
        "UGRADERT, false",
        "FORTROLIG, true",
        "STRENGT_FORTROLIG, true",
        "STRENGT_FORTROLIG_UTLAND, true",
    )
    fun adressebeskyttelse(
        gradering: String,
        forventet: Boolean,
    ) {
        val mockEngine =
            MockEngine { request ->
                respond(
                    pdlResponse(gradering),
                    headers = headersOf("Content-Type", "application/json"),
                )
            }

        runBlocking {
            PDLHttpKlient(
                url = "http://localhost:8080",
                tokenSupplier = { "token" },
                httpClient =
                    defaultHttpClient(
                        mockEngine,
                    ),
            ).erAdressebeskyttet("12345612345").getOrThrow() shouldBe forventet
        }
    }

    @Test
    fun `Skal returnere failure dersom vi ikke finner personen`() {
        val mockEngine =
            MockEngine { request ->
                respond(
                    """{"errors": [{"message": "Fant ikke person med identifikator 12345612345"}]}""",
                    headers = headersOf("Content-Type", "application/json"),
                )
            }

        runBlocking {
            PDLHttpKlient(
                url = "http://localhost:8080",
                tokenSupplier = { "token" },
                httpClient =
                    defaultHttpClient(
                        mockEngine,
                    ),
            ).erAdressebeskyttet("12345612345").isFailure shouldBe true
        }
    }

    @Test
    fun `Skal returnere failure dersom respons fra PDL ikke er 200`() {
        val mockEngine =
            MockEngine { request ->
                respondBadRequest()
            }

        runBlocking {
            PDLHttpKlient(
                url = "http://localhost:8080",
                tokenSupplier = { "token" },
                httpClient =
                    defaultHttpClient(
                        mockEngine,
                    ),
            ).erAdressebeskyttet("12345612345").isFailure shouldBe true
        }
    }

    private fun pdlResponse(gradering: String): String {
        val jsonString = "/pdlresponse.json".fileAsText()

        val pattern = """"gradering"\s*:\s*"([^"]*)"""".toRegex()
        val replacement = """"gradering": "$gradering""""
        return jsonString.replace(pattern, replacement)
    }
}
