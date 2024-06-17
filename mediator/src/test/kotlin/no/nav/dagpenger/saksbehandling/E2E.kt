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
        val token =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6IkwxS2ZLRklfam5YYndXYzIyeFp4dzFzVUhIMCJ9.eyJhdWQiOiJiZmY2MmRjYi04OGRjLTRhMTMtOWUyNi0xY2JmZmI5NmVjNjQiLCJpc3MiOiJodHRwczovL2xvZ2luLm1pY3Jvc29mdG9ubGluZS5jb20vOTY2YWM1NzItZjViNy00YmJlLWFhODgtYzc2NDE5YzBmODUxL3YyLjAiLCJpYXQiOjE3MTg2MDk5MzcsIm5iZiI6MTcxODYwOTkzNywiZXhwIjoxNzE4NjE0Mzc3LCJhaW8iOiJBVVFBdS84WEFBQUEzai9DQSsxcUtTNVcrR1REZTBJNmd5Wm04N05VWndsT0xSRnhKRGJvSmJQRi9hVTNJNDAxTDFMTVhxVnBITmRXM3NvbmJRaGZhR2kvUEdza21YdElmUT09IiwiYXpwIjoiNWYyOGMxMGYtZWNkNS00NWVmLTk2YjgtZGI0MzExODAyZTg5IiwiYXpwYWNyIjoiMSIsImdyb3VwcyI6WyIxMWI4NDc1YS1mYjEyLTQxYWEtYjFmNi04NDk3YzFiNTM4NWIiLCIzZTI4NDY2Zi1jNTNkLTQ2ZGEtOGI0NC1hNGFiYzJhZDQ1OTMiXSwibmFtZSI6IkZfWjk5MzI5OCBFX1o5OTMyOTgiLCJvaWQiOiI3OTlhMTMyYS05M2M4LTQyN2UtOTJmNy0wOGNiMjIwYzg3OWEiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJGX1o5OTMyOTguRV9aOTkzMjk4QHRyeWdkZWV0YXRlbi5ubyIsInJoIjoiMC5BVWNBY3NWcWxyZjF2a3VxaU1ka0djRDRVY3N0OXJfY2lCTktuaVljdl91VzdHUU5BV0kuIiwic2NwIjoiZGVmYXVsdGFjY2VzcyIsInN1YiI6IlFET3lPUzBIRjRjUlZRTjdHYl92SVctQ1d1MnZJUEpfbmJfeXBGUkJtX28iLCJ0aWQiOiI5NjZhYzU3Mi1mNWI3LTRiYmUtYWE4OC1jNzY0MTljMGY4NTEiLCJ1dGkiOiJBRFpWN2RVek1FdXZLVUd5V2w5ZUFBIiwidmVyIjoiMi4wIiwiTkFWaWRlbnQiOiJaOTkzMjk4IiwiYXpwX25hbWUiOiJkZXYtZ2NwOmF1cmE6YXp1cmUtdG9rZW4tZ2VuZXJhdG9yIn0.VxwtD8V0DeS28FZf42bOMZM9emJd0nOOjmHTWhBJfEN64Q91mlwDSz3kcEZvhPz7QjVDMz6HzXtX1Q4h5qERPWiZ_jLqfsPEoVLau0PU6u66-fyCbICpzGDpJDFysP7fZ26ck9FJpy0kMDz5915--6KFLither3xPcw3uNOTVpK2MfHC_uygp_rKSpUUgANOdifnc9QperxDLhISu8vlCXZkVKYbPsVMlgEAsp9LEwEdC-SiVf-8QcaUx6ZeDs8th8YlJ4_s9buHh1qZY5lrsGkA9ApMksXIjrz_KGBM9NjfyLJFdaOfGnANHrk1VStr9-VCz7Tn6LeIhQ5Vx9r77Q"
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