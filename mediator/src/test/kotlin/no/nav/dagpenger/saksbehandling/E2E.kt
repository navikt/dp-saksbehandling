package no.nav.dagpenger.saksbehandling

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

class E2E {
    @Test
    @Disabled
    fun sendBrev() {
        val token = ""
        val oppgaveId = "018fe853-feca-717b-9e10-cf794ab489d3"
        runBlocking {
            val response =
                plainHttpClient.post("https://dp-saksbehandling.intern.dev.nav.no/utsending/{$oppgaveId}/send-brev") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    setBody("""<H1>Hei</H1><p>Her er et brev</p>""")
                    contentType(ContentType.Text.Html)
                }
            println(response)
        }
    }

    val plainHttpClient =
        HttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = 130.seconds.inWholeMilliseconds
                connectTimeoutMillis = 130.seconds.inWholeMilliseconds
                socketTimeoutMillis = 130.seconds.inWholeMilliseconds
            }
        }
}
