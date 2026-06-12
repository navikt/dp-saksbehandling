package no.nav.dagpenger.saksbehandling.meldekortkontroll

import com.fasterxml.jackson.annotation.JsonInclude
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
import io.ktor.serialization.jackson3.jackson
import io.prometheus.metrics.model.registry.PrometheusRegistry
import no.nav.dagpenger.ktor.client.metrics.PrometheusMetricsPlugin
import java.util.UUID


class MeldekortKontrollKlient(
    private val meldkortKontrollUrl: String,
    private val tokenProvider: () -> String,
    private val httpClient: HttpClient = httpClient(),
) {
    suspend fun harAvvikendeMeldkortSyklus(ident: String, søknadId: UUID): Result<Boolean> {
        return httpClient.post(meldkortKontrollUrl) {
            header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke()}")
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(
                MeldekortKontrollRequest(ident, søknadId)
            )
        }.let {
            when (it.status.value) {
                in 200..299 -> Result.success(it.body<MeldekortKontrollResponse>().harAvvikendeMeldkortSyklus)
                404 -> Result.success(false)
                else -> Result.failure(RuntimeException("Kall til meldekortkontroll feilet med status ${it.status}"))
            }
        }
    }
}

private data class MeldekortKontrollRequest(
    val ident: String,
    val søknadId: UUID,
)

private data class MeldekortKontrollResponse(
    val harAvvikendeMeldkortSyklus: Boolean,
)

private fun httpClient(
    prometheusRegistry: PrometheusRegistry = PrometheusRegistry.defaultRegistry,
    engine: HttpClientEngine = CIO.create { },
) = HttpClient(engine) {
    expectSuccess = false
    install(ContentNegotiation) {
        jackson { }
    }

    install(PrometheusMetricsPlugin) {
        this.baseName = "dp_saksbehandling_dp_meldekortregister_http_klient"
        this.registry = prometheusRegistry
    }
}
