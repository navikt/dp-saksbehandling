package no.nav.dagpenger.saksbehandling.skjerming

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.prometheus.metrics.model.registry.PrometheusRegistry

private val logger = KotlinLogging.logger { }

internal class SkjermingHttpKlient(
    private val skjermingApiUrl: String,
    private val tokenProvider: () -> String,
    private val httpClient: HttpClient = lagSkjermingHttpKlient(),
) : SkjermingKlient {
    companion object {
        fun lagSkjermingHttpKlient(
            engine: HttpClientEngine = CIO.create {},
            registry: PrometheusRegistry = PrometheusRegistry.defaultRegistry,
        ): HttpClient {
            return createHttpClient(
                engine = engine,
                metricsBaseName = "dp_saksbehandling_skjerming_http_klient",
                prometheusRegistry = registry,
            )
        }
    }

    override suspend fun erSkjermetPerson(ident: String): Result<Boolean> {
        return kotlin.runCatching {
            httpClient.post(urlString = skjermingApiUrl) {
                header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke()}")
                contentType(ContentType.Application.Json)
                accept(ContentType.Text.Plain)
                setBody(SkjermingRequest(ident))
            }.bodyAsText().toBoolean()
        }.onFailure { throwable -> logger.error(throwable) { "Kall til skjerming feilet" } }
    }

    private data class SkjermingRequest(val personident: String)
}
