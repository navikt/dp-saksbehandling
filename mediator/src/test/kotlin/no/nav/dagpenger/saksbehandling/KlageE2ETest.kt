
package no.nav.dagpenger.saksbehandling

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class KlageE2ETest {
    val plainHttpClient =
        HttpClient {
        }

    @Suppress("ktlint:standard:max-line-length")
    private val token =
        ""

    @Test
    @Disabled
    fun `KlageE2ETest`() {
        runBlocking {
            plainHttpClient.post("https://dp-saksbehandling.intern.dev.nav.no/klage/opprett") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.ContentType, "application/json")
                //language=json
                setBody(
                    """
                    {
                        "journalpostId": "1234",
                        "opprettet": "${LocalDateTime.now()}",
                        "sakId": "sakId",
                        "personIdent": {"ident":  "20885598405"}
                    }
                    """.trimIndent(),
                )
            }.also { println(it.bodyAsText()) }
        }
    }
}
