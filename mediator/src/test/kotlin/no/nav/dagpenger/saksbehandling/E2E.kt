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
        val token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ik1HTHFqOThWTkxvWGFGZnBKQ0JwZ0I0SmFLcyJ9.eyJhdWQiOiJiZmY2MmRjYi04OGRjLTRhMTMtOWUyNi0xY2JmZmI5NmVjNjQiLCJpc3MiOiJodHRwczovL2xvZ2luLm1pY3Jvc29mdG9ubGluZS5jb20vOTY2YWM1NzItZjViNy00YmJlLWFhODgtYzc2NDE5YzBmODUxL3YyLjAiLCJpYXQiOjE3MTkyMzIyOTQsIm5iZiI6MTcxOTIzMjI5NCwiZXhwIjoxNzE5MjM3Nzg3LCJhaW8iOiJBV1FBbS84WEFBQUFualA2NkI3MDYrdzlycGJnbWdDVVB6bTVOUW93WitSWHZpcWJ5NGhVbHZiZXZrcW03bWU4VUhZTzFkOUZzc0V4NVloMWRtTUVrbkRMeEtUeTZzdXEwT2RjZEpXekx1YTgvYmlsNWZlRFBmeDJ0U0ZwVTh1ZWpGcG5zc0M2WHRHNyIsImF6cCI6IjVmMjhjMTBmLWVjZDUtNDVlZi05NmI4LWRiNDMxMTgwMmU4OSIsImF6cGFjciI6IjEiLCJncm91cHMiOlsiMTFiODQ3NWEtZmIxMi00MWFhLWIxZjYtODQ5N2MxYjUzODViIiwiM2UyODQ2NmYtYzUzZC00NmRhLThiNDQtYTRhYmMyYWQ0NTkzIl0sIm5hbWUiOiJGX1o5OTQ4NTQgRV9aOTk0ODU0Iiwib2lkIjoiMDE5MDljNDctZTVmOC00YmRiLWI3Y2EtODBlYzhhZGE5ZGQ4IiwicHJlZmVycmVkX3VzZXJuYW1lIjoiRl9aOTk0ODU0LkVfWjk5NDg1NEB0cnlnZGVldGF0ZW4ubm8iLCJyaCI6IjAuQVVjQWNzVnFscmYxdmt1cWlNZGtHY0Q0VWNzdDlyX2NpQk5LbmlZY3ZfdVc3R1FOQVdNLiIsInNjcCI6ImRlZmF1bHRhY2Nlc3MiLCJzdWIiOiJrWEVSazNTbjU2WkgzelR2RWIwSXhwZmxMZFdvTXp1bERPNnMxZkNEZG93IiwidGlkIjoiOTY2YWM1NzItZjViNy00YmJlLWFhODgtYzc2NDE5YzBmODUxIiwidXRpIjoicDR0TXh6X053RUNtdXlVck44SXhBQSIsInZlciI6IjIuMCIsIk5BVmlkZW50IjoiWjk5NDg1NCIsImF6cF9uYW1lIjoiZGV2LWdjcDphdXJhOmF6dXJlLXRva2VuLWdlbmVyYXRvciJ9.KuSyL2J8i8uXXaKcYVSzMPMth_grV7d0-R1ip2Q_kKhCcuKUtpWHtFWXmmj43oIvgr0xQnQQLMqIkMm1PpLMcQVUm8sUBvyZodTc1zQeYSOvIhgZNAFu3ZAFf_kO_aTJSdjDmzR5iv1SDafCGkiTk-zAdcH8K9SXgqt0107GfzWD9j6-s4nReptIutV7ML5Qt650awZXgCJuyo-qQovjTueFQz3yD3FJz_Tn5XyAFlgj-HWw6CLmcRfnHwIfQPI3qKiqW0F8lTfFK0Y3ibrENiVsiO2F6SCOv3ohTkzxyO5i-sy6wRlYMrGdGzJBiyvSQSdPW1w_fHsTk9N5wknLOQ"
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
