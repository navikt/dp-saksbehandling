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
import io.prometheus.client.CollectorRegistry
import mu.KotlinLogging
import no.nav.dagpenger.ktor.client.metrics.PrometheusMetricsPlugin
import java.util.UUID

private val logger = KotlinLogging.logger {}

class JournalpostIdHttpClient(
    private val journalpostIdApiUrl: String,
    private val tokenProvider: () -> String,
    private val httpClient: HttpClient = httpClient(),
) : JournalpostIdClient {
    override suspend fun hentJournalpostId(søknadId: UUID): Result<String> {
        val urlString = "$journalpostIdApiUrl/$søknadId"
        logger.info { "Henter journalpostId fra $urlString" }

        return kotlin.runCatching {
            httpClient.get(urlString = urlString) {
                header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke()}")
                accept(ContentType.Text.Plain)
            }.bodyAsText()
        }.onFailure { throwable ->
            logger.error(throwable) { "Kall til journalpostId-api feilet for søknad med id: $søknadId" }
        }
    }
}

fun httpClient(
    collectorRegistry: CollectorRegistry = CollectorRegistry.defaultRegistry,
    engine: HttpClientEngine = CIO.create { },
) = HttpClient(engine) {
    expectSuccess = true

    install(PrometheusMetricsPlugin) {
        this.baseName = "dp_saksbehandling_joark_http_klient"
        this.registry = collectorRegistry
    }
}
