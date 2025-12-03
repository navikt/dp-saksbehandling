package no.nav.dagpenger.saksbehandling.behandling

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
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
import no.nav.dagpenger.saksbehandling.skjerming.createHttpClient
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private val logger = KotlinLogging.logger {}

interface BehandlingKlient {
    fun godkjenn(
        behandlingId: UUID,
        ident: String,
        saksbehandlerToken: String,
    ): Result<Unit>

    suspend fun kreverTotrinnskontroll(
        behandlingId: UUID,
        saksbehandlerToken: String,
    ): Result<Boolean>

    fun sendTilbake(
        behandlingId: UUID,
        ident: String,
        saksbehandlerToken: String,
    ): Result<Unit>

    fun beslutt(
        behandlingId: UUID,
        ident: String,
        saksbehandlerToken: String,
    ): Result<Unit>

    fun avbryt(
        behandlingId: UUID,
        ident: String,
        saksbehandlerToken: String,
    ): Result<Unit>

    fun opprettManuellBehandling(
        personIdent: String,
        saksbehandlerToken: String,
    ): Result<UUID>
}

internal class BehandlingHttpKlient(
    private val dpBehandlingApiUrl: String,
    private val tokenProvider: (String) -> String,
    private val httpClient: HttpClient = lagBehandlingHttpKlient(),
) : BehandlingKlient {
    companion object {
        fun lagBehandlingHttpKlient(
            engine: HttpClientEngine = CIO.create {},
            registry: PrometheusRegistry = PrometheusRegistry.defaultRegistry,
            metricsBaseName: String = "dp_saksbehandling_behandling_http_klient",
            timeOut: Duration = 2000.milliseconds,
        ): HttpClient {
            return createHttpClient(
                engine = engine,
                metricsBaseName = metricsBaseName,
                prometheusRegistry = registry,
                expectSuccess = false,
            ) {
                install(HttpTimeout) {
                    requestTimeoutMillis = timeOut.inWholeMilliseconds
                }
            }
        }
    }

    override fun godkjenn(
        behandlingId: UUID,
        ident: String,
        saksbehandlerToken: String,
    ): Result<Unit> {
        return kallBehandling("godkjenn", behandlingId, saksbehandlerToken, ident)
    }

    override fun sendTilbake(
        behandlingId: UUID,
        ident: String,
        saksbehandlerToken: String,
    ): Result<Unit> {
        return kallBehandling("send-tilbake", behandlingId, saksbehandlerToken, ident)
    }

    override fun beslutt(
        behandlingId: UUID,
        ident: String,
        saksbehandlerToken: String,
    ): Result<Unit> {
        return kallBehandling("beslutt", behandlingId, saksbehandlerToken, ident)
    }

    override fun avbryt(
        behandlingId: UUID,
        ident: String,
        saksbehandlerToken: String,
    ): Result<Unit> {
        return kallBehandling("avbryt", behandlingId, saksbehandlerToken, ident)
    }

    override fun opprettManuellBehandling(
        personIdent: String,
        saksbehandlerToken: String,
    ): Result<UUID> {
        return runBlocking {
            runCatching {
                httpClient.post("$dpBehandlingApiUrl/person/behandling") {
                    header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke(saksbehandlerToken)}")
                    header(HttpHeaders.ContentType, ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    setBody(Request(personIdent))
                }.body<BehandlingDTO>().behandlingId.let { UUID.fromString(it) }
            }
        }.onFailure {
            logger.error(it) { "Kall til dp-behandling for å opprette manuell behandling feilet ${it.message}" }
        }
    }

    override suspend fun kreverTotrinnskontroll(
        behandlingId: UUID,
        saksbehandlerToken: String,
    ): Result<Boolean> {
        return kotlin.runCatching {
            httpClient.get(urlString = "$dpBehandlingApiUrl/behandling/$behandlingId") {
                header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke(saksbehandlerToken)}")
                accept(ContentType.Application.Json)
            }.body<BehandlingDTO>().let { behandlingDTO ->
                behandlingDTO.kreverTotrinnskontroll
            }
        }
            .onFailure { logger.error(it) { "Kall til dp-behandling for å hente kreverTotrinnskontroll feilet ${it.message}" } }
    }

    private fun kallBehandling(
        endepunkt: String,
        behandlingId: UUID,
        saksbehandlerToken: String,
        ident: String,
    ): Result<Unit> {
        val urlString = "$dpBehandlingApiUrl/behandling/$behandlingId/$endepunkt"
        return runBlocking {
            try {
                httpClient.post(urlString = urlString) {
                    header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke(saksbehandlerToken)}")
                    header(HttpHeaders.ContentType, ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    setBody(Request(ident))
                }.let {
                    val statuskode = it.status.value
                    logger.info { "Kall til dp-behandling for $endepunkt returnerte status $statuskode" }
                    when (statuskode) {
                        in 200..299 -> Result.success(Unit)
                        else -> Result.failure(BehandlingException(it.bodyAsText(), it.status.value))
                    }
                }
            } catch (e: Exception) {
                logger.error { "Feil mot dp-behandling for endepunkt: $urlString med ${e.message}" }
                Result.failure(BehandlingException(e.message, 500))
            }
        }
    }
}

data class BehandlingException(val text: String?, val status: Int) : RuntimeException(
    "Feil ved kall mot dp-behandling: $text, status: $status",
)

class BehandlingKreverIkkeTotrinnskontrollException(message: String) : RuntimeException(message)

private data class Request(val ident: String)

private data class BehandlingDTO(
    val behandlingId: String,
    val kreverTotrinnskontroll: Boolean,
)
