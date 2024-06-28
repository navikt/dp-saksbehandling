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
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

class E2E {
    @Test
//    @Disabled
    fun sendBrev() {
        val token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ik1HTHFqOThWTkxvWGFGZnBKQ0JwZ0I0SmFLcyJ9.eyJhdWQiOiJiZmY2MmRjYi04OGRjLTRhMTMtOWUyNi0xY2JmZmI5NmVjNjQiLCJpc3MiOiJodHRwczovL2xvZ2luLm1pY3Jvc29mdG9ubGluZS5jb20vOTY2YWM1NzItZjViNy00YmJlLWFhODgtYzc2NDE5YzBmODUxL3YyLjAiLCJpYXQiOjE3MTk1NjA5MzIsIm5iZiI6MTcxOTU2MDkzMiwiZXhwIjoxNzE5NTY2NTQxLCJhaW8iOiJBVVFBdS84WEFBQUFHWFgraXQyQ3JFZS8xUG56aFNZaHg4eXhBU3NJT1o5a2FYQjdUdlA1dHpWTlFWdWI4RFdWNmxsdmU2eFdnWHE0NVdyUlFsaVdDVU5CMzNEeXJXK2VSdz09IiwiYXpwIjoiNWYyOGMxMGYtZWNkNS00NWVmLTk2YjgtZGI0MzExODAyZTg5IiwiYXpwYWNyIjoiMSIsImdyb3VwcyI6WyIxMWI4NDc1YS1mYjEyLTQxYWEtYjFmNi04NDk3YzFiNTM4NWIiLCIzZTI4NDY2Zi1jNTNkLTQ2ZGEtOGI0NC1hNGFiYzJhZDQ1OTMiXSwibmFtZSI6IkZfWjk5MzI5OCBFX1o5OTMyOTgiLCJvaWQiOiI3OTlhMTMyYS05M2M4LTQyN2UtOTJmNy0wOGNiMjIwYzg3OWEiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJGX1o5OTMyOTguRV9aOTkzMjk4QHRyeWdkZWV0YXRlbi5ubyIsInJoIjoiMC5BVWNBY3NWcWxyZjF2a3VxaU1ka0djRDRVY3N0OXJfY2lCTktuaVljdl91VzdHUU5BV0kuIiwic2NwIjoiZGVmYXVsdGFjY2VzcyIsInN1YiI6IlFET3lPUzBIRjRjUlZRTjdHYl92SVctQ1d1MnZJUEpfbmJfeXBGUkJtX28iLCJ0aWQiOiI5NjZhYzU3Mi1mNWI3LTRiYmUtYWE4OC1jNzY0MTljMGY4NTEiLCJ1dGkiOiJhMGZ4WnpPWUgwdUpyX0JWRUFwSkFBIiwidmVyIjoiMi4wIiwiTkFWaWRlbnQiOiJaOTkzMjk4IiwiYXpwX25hbWUiOiJkZXYtZ2NwOmF1cmE6YXp1cmUtdG9rZW4tZ2VuZXJhdG9yIn0.tREvrrIv2N7wm_T6Vcs7o6gmQMyEWCx0aZH1vUgRsRaRLDa7wRiZCU83XagvZhgoFLtMOVplMuNVNoJK-BHb7lLn1xdeChxu1UQy7dg7EBTT2kiXo2k0KhEFBg8Zut9IJ5trs5YRCQGs5hJuebim-lEbXXwObb3QuEGLMAIFUapUvrLH83j5B93y742gCORR-hT0tdk3Q47bIDsyU-tZWF8aMe1wS6DsdY4aOOXgTMiEw8z0EfL8nxjwzZP2mD3XywKd4eqyzz_fkvrfBzUjEfV5LHPMyfe41L2mfeIYB7B4bF2li9Oa2sF6kBhqa23gO49eXW4FVAPLWGfebpUB-Q"
        val oppgaveId = "01904f17-3594-712d-ade9-9b1c2f1b5f6b"
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
