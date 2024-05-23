package no.nav.dagpenger.saksbehandling.journalpostid

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import mu.KotlinLogging
import java.util.UUID

private val logger = KotlinLogging.logger {}

class JournalpostIdHttpClient(
    private val journalpostIdApiUrl: String,
    private val tokenProvider: () -> String,
    private val httpClient: HttpClient = httpClient(),
) : JournalpostIdClient {
    override suspend fun hentJournalpostId(søknadId: UUID): Result<String> {
        logger.info { "Henter journalpostId for søknad med id: $søknadId" }

        return kotlin.runCatching {
            httpClient.get(urlString = journalpostIdApiUrl) {
                header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke()}")
                accept(ContentType.Text.Plain)
            }.bodyAsText()
        }.onFailure { throwable -> logger.error(throwable) { "Kall til journalpostId-api feilet." } }
    }
}

fun httpClient(engine: HttpClientEngine = CIO.create { }) =
    HttpClient(engine) {
        expectSuccess = true
    }
