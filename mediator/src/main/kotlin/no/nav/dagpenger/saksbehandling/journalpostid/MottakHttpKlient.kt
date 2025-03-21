package no.nav.dagpenger.saksbehandling.journalpostid

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.jackson.jackson
import io.prometheus.metrics.model.registry.PrometheusRegistry
import mu.KotlinLogging
import no.nav.dagpenger.ktor.client.metrics.PrometheusMetricsPlugin
import java.util.UUID

private val logger = KotlinLogging.logger {}

class MottakHttpKlient(
    private val journalpostIdApiUrl: String,
    private val tokenProvider: () -> String,
    private val httpClient: HttpClient = httpClient(),
) : JournalpostIdClient {
    override suspend fun hentJournalpostIder(
        søknadId: UUID,
        ident: String,
    ): Result<List<String>> {
        return kotlin.runCatching {
            httpClient.post(urlString = "$journalpostIdApiUrl/v1/journalpost/sok") {
                header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke()}")
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(
                    JournalpostSok(
                        soknadId = søknadId.toString(),
                        ident = ident,
                    ),
                )
            }.body<JournalpostIder>().journalpostIder
        }.onFailure { throwable ->
            logger.error(throwable) { "Kall til dp-mottak api feilet for søknad med id: $søknadId" }
        }
    }
}

private data class JournalpostSok(
    val soknadId: String,
    val ident: String,
)

private data class JournalpostIder(
    val journalpostIder: List<String>,
)

fun httpClient(
    prometheusRegistry: PrometheusRegistry = PrometheusRegistry.defaultRegistry,
    engine: HttpClientEngine = CIO.create { },
) = HttpClient(engine) {
    expectSuccess = true

    install(ContentNegotiation) {
        jackson {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            setSerializationInclusion(JsonInclude.Include.NON_NULL)
        }
    }

    install(PrometheusMetricsPlugin) {
        this.baseName = "dp_saksbehandling_dp_mottak_http_klient"
        this.registry = prometheusRegistry
    }
}
