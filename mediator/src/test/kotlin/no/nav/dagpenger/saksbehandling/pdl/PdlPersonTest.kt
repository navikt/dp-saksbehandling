package no.nav.dagpenger.saksbehandling.pdl

import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.io.FileNotFoundException

class PdlPersonTest {
    @ParameterizedTest()
    @CsvSource(
        "UGRADERT, false",
        "FORTROLIG, true",
        "STRENGT_FORTROLIG, true",
        "STRENGT_FORTROLIG_UTLAND, true",
    )

    fun `Adressebeskyttelse`(gradering: String, forventet: Boolean) {
        val mockEngine = MockEngine { request ->
            respond(
                pdlResponse(gradering),
                headers = headersOf("Content-Type", "application/json"),
            )
        }

        runBlocking {
            PdlPerson(
                url = "http://localhost:8080",
                tokenSupplier = { "token" },
                httpClient = defaultHttpClient(
                    mockEngine,
                ),
            ).erAdressebeskyttet("12345612345").getOrThrow() shouldBe forventet
        }
    }

    private fun pdlResponse(gradering: String): String {
        val jsonString = "/pdlresponse.json".fileAsText()

        val pattern = """"gradering"\s*:\s*"([^"]*)"""".toRegex()
        val replacement = """"gradering": "$gradering""""
        return jsonString.replace(pattern, replacement)
    }

    private fun String.fileAsText(): String {
        return object {}.javaClass.getResource(this)?.readText()
            ?: throw FileNotFoundException()
    }
}
