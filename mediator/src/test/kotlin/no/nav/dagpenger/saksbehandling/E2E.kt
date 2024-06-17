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
    fun sendBrev() {
        val token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6IkwxS2ZLRklfam5YYndXYzIyeFp4dzFzVUhIMCJ9.eyJhdWQiOiJiZmY2MmRjYi04OGRjLTRhMTMtOWUyNi0xY2JmZmI5NmVjNjQiLCJpc3MiOiJodHRwczovL2xvZ2luLm1pY3Jvc29mdG9ubGluZS5jb20vOTY2YWM1NzItZjViNy00YmJlLWFhODgtYzc2NDE5YzBmODUxL3YyLjAiLCJpYXQiOjE3MTg2MjY5MjMsIm5iZiI6MTcxODYyNjkyMywiZXhwIjoxNzE4NjMwOTAzLCJhaW8iOiJBVVFBdS84WEFBQUFDWmREcTRISXZCR0tuVHFibGwrQlF4ckpVRjhJNjd0cDIrMXRnNkJEWHdqdnBsNCsvWk05MlN0TjhFV1Fid0J6ZWtsaEdBb1FYcjNOb0VsMUZDNHg2UT09IiwiYXpwIjoiNWYyOGMxMGYtZWNkNS00NWVmLTk2YjgtZGI0MzExODAyZTg5IiwiYXpwYWNyIjoiMSIsImdyb3VwcyI6WyIxMWI4NDc1YS1mYjEyLTQxYWEtYjFmNi04NDk3YzFiNTM4NWIiLCIzZTI4NDY2Zi1jNTNkLTQ2ZGEtOGI0NC1hNGFiYzJhZDQ1OTMiXSwibmFtZSI6IkZfWjk5MzI5OCBFX1o5OTMyOTgiLCJvaWQiOiI3OTlhMTMyYS05M2M4LTQyN2UtOTJmNy0wOGNiMjIwYzg3OWEiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJGX1o5OTMyOTguRV9aOTkzMjk4QHRyeWdkZWV0YXRlbi5ubyIsInJoIjoiMC5BVWNBY3NWcWxyZjF2a3VxaU1ka0djRDRVY3N0OXJfY2lCTktuaVljdl91VzdHUU5BV0kuIiwic2NwIjoiZGVmYXVsdGFjY2VzcyIsInN1YiI6IlFET3lPUzBIRjRjUlZRTjdHYl92SVctQ1d1MnZJUEpfbmJfeXBGUkJtX28iLCJ0aWQiOiI5NjZhYzU3Mi1mNWI3LTRiYmUtYWE4OC1jNzY0MTljMGY4NTEiLCJ1dGkiOiJ5ejJxZmdaUkRVcVpZWFBjYm42VUFBIiwidmVyIjoiMi4wIiwiTkFWaWRlbnQiOiJaOTkzMjk4IiwiYXpwX25hbWUiOiJkZXYtZ2NwOmF1cmE6YXp1cmUtdG9rZW4tZ2VuZXJhdG9yIn0.hRQBNmJ-Y5yyxDvxNC_aRjKuqV8GypQIGc8OnJ-myIFH4XnWIzNEoNuMohmNu-hPca5RxZua9tFaEPQSlsg8K5BMEdNp4lHf6qf3ngd4m2Fw--u7v2f3v-X154De0wbvXXJp1McTjG7thSgHYqj2DCW3wZym13Qmdi4SeQIRHl0PSzIcpjwxf66l10I4yMwILqf5iy-90wKRKErhok_UhS5JEh5kKYXdabl1vCAhFaS-LRzO53k-G1wLwXxicbxwzgiHI1_WMZ2sryf-eIG4dFbrAqRO2aDhogldmN1cA_rPqbOUhpjd1wY2AAA02q4XO9NHEHOGdE9MvuswfOV-Jg"
        val oppgaveId = "018fe84c-08d8-7bd8-8847-9091ab724da8"
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
