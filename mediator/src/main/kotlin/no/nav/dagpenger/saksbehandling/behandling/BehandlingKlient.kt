package no.nav.dagpenger.saksbehandling.behandling

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
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
        }.onFailure { logger.error(it) { "Kall til dp-behandling feilet ${it.message}" } }
    }
}

class GodkjennBehandlingFeiletException(message: String) : RuntimeException(message)

private data class Request(val ident: String)
