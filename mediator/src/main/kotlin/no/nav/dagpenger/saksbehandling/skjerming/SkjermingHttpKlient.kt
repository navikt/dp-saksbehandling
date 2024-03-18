package no.nav.dagpenger.saksbehandling.skjerming

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import no.nav.dagpenger.saksbehandling.createHttpClient

internal class SkjermingHttpKlient(
    private val skjermingApiUrl: String,
    private val tokenProvider: () -> String,
    private val httpClient: HttpClient = createHttpClient(engine = CIO.create { }),
) : SkjermingKlient {

    override suspend fun egenAnsatt(id: String): Result<Boolean> {
        return kotlin.runCatching {
            httpClient.post(urlString = skjermingApiUrl) {
                header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke()}")
                accept(ContentType.Application.Json)
                setBody("""{"personident":"$id"}""")
            }.bodyAsText().toBoolean()
        }
    }
}
