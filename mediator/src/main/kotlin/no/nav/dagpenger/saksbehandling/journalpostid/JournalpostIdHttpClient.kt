package no.nav.dagpenger.saksbehandling.journalpostid

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import mu.KotlinLogging
import java.util.UUID

private val logger = KotlinLogging.logger {}

class JournalpostIdHttpClient(
    private val journalpostIdApiUrl: String,
    private val tokenProvider: () -> String,
    private val httpClient: HttpClient = httpClient(),
) : JournalpostIdClient {
    override fun hentJournalpostId(søknadId: UUID): Result<String> {
        logger.info { "Henter journalpostId for søknad med id: $søknadId" }
        TODO("Not yet implemented")
    }
}

fun httpClient(engine: HttpClientEngine = CIO.create { }) =
    HttpClient(engine) {
        expectSuccess = false
    }
