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
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

class E2E {
    @Test
    @Disabled
    fun sendBrev() {
        val token = ""
        val oppgaveId = "01904a3d-b2a1-7d42-a744-3b28c3c31f46"
        runBlocking {
            val response =
                plainHttpClient.post("https://dp-saksbehandling.intern.dev.nav.no/utsending/$oppgaveId/send-brev") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    setBody(enkeltHtml)
                    contentType(ContentType.Text.Html)
                }
            println(response)
        }
    }

    @Language("HTML")
    val enkeltHtml =
        """
        <html lang="no">
        <head><title>Enkelt</title>
            <style>body {
                font-family: 'Source Sans Pro';
                font-style: normal;
                width: 600px;
                padding: 0 40px 40px 40px;
                color: rgb(38, 38, 38);
            }

            h1 {
                font-weight: 600;
                font-size: 32px;
                line-height: 40px;
            }
            </style>
        </head>
        <body>
        <h1>Hello world !</h1>
        </body>
        </html>   
        """.trimIndent()

    val plainHttpClient =
        HttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = 130.seconds.inWholeMilliseconds
                connectTimeoutMillis = 130.seconds.inWholeMilliseconds
                socketTimeoutMillis = 130.seconds.inWholeMilliseconds
            }
        }
}
