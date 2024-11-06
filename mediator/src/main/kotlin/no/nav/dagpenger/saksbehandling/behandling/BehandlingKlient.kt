package no.nav.dagpenger.saksbehandling.behandling

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.prometheus.metrics.model.registry.PrometheusRegistry
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.skjerming.createHttpClient
import java.util.UUID

private val logger = KotlinLogging.logger {}

interface BehandlingKlient {
    fun godkjennBehandling(
        behandlingId: UUID,
        ident: String,
        saksbehandlerToken: String,
    ): Result<Unit>

    suspend fun kreverTotrinnskontroll(
        behandlingId: UUID,
        saksbehandlerToken: String,
    ): Result<Boolean>
}

internal class BehandlngHttpKlient(
    private val dpBehandlingApiUrl: String,
    private val tokenProvider: (String) -> String,
    private val httpClient: HttpClient = lagBehandlingHttpKlient(),
) : BehandlingKlient {
    companion object {
        fun lagBehandlingHttpKlient(
            engine: HttpClientEngine = CIO.create {},
            registry: PrometheusRegistry = PrometheusRegistry.defaultRegistry,
        ): HttpClient {
            return createHttpClient(
                engine = engine,
                metricsBaseName = "dp_saksbehandling_behandling_http_klient",
                prometheusRegistry = registry,
            )
        }
    }

    override fun godkjennBehandling(
        behandlingId: UUID,
        ident: String,
        saksbehandlerToken: String,
    ): Result<Unit> {
        return kotlin.runCatching {
            runBlocking {
                httpClient.post(urlString = "$dpBehandlingApiUrl/$behandlingId/godkjenn") {
                    header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke(saksbehandlerToken)}")
                    header(HttpHeaders.ContentType, ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    setBody(Request(ident))
                }
            }.let {}
        }.onFailure { logger.error(it) { "Kall til dp-behandling feilet ved godkjenning ${it.message}" } }
    }

    override suspend fun kreverTotrinnskontroll(
        behandlingId: UUID,
        saksbehandlerToken: String,
    ): Result<Boolean> {
        return kotlin.runCatching {
            runBlocking {
                httpClient.get(urlString = "$dpBehandlingApiUrl/$behandlingId") {
                    header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke(saksbehandlerToken)}")
                    header(HttpHeaders.ContentType, ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                }
            }.let { response ->
                val objectMapper = ObjectMapper()
                objectMapper.readTree(response.bodyAsText())["kreverTotrinnskontroll"].asBoolean()
            }
        }.onFailure { logger.error(it) { "Kall til dp-behandling feilet ved henting av kreverTotrinnskontroll ${it.message}" } }
    }
}

class GodkjennBehandlingFeiletException(message: String) : RuntimeException(message)

private data class Request(val ident: String)
