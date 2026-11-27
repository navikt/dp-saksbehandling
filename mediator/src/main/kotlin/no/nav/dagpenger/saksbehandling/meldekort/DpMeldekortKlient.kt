package no.nav.dagpenger.saksbehandling.meldekort

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

class DpMeldekortKlient(
    private val dpMeldekortApiUrl: String,
    private val tokenProvider: () -> String,
    private val httpClient: HttpClient = lagDpMeldekortHttpKlient(),
) {
    suspend fun harMeldekortMedEndretMeldesyklus(
        ident: String,
        søknadId: UUID,
    ): Result<Boolean> =
        httpClient
            .post(dpMeldekortApiUrl) {
                header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke()}")
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(
                    HarBrukerMeldekortMedEndretMeldesyklusRequest(ident, søknadId),
                )
            }.let {
                when (it.status.value) {
                    in 200..299 ->
                        Result.success(
                            it.body<HarBrukerMeldekortMedEndretMeldesyklusResponse>().harMeldekortMedEndretMeldesyklus,
                        )
                    404 -> Result.success(false)
                    else -> Result.failure(RuntimeException("Kall til dp-meldekort feilet med status ${it.status}"))
                }
            }
}

private data class HarBrukerMeldekortMedEndretMeldesyklusRequest(
    val ident: String,
    val søknadId: UUID,
)

private data class HarBrukerMeldekortMedEndretMeldesyklusResponse(
    val harMeldekortMedEndretMeldesyklus: Boolean,
)

internal fun lagDpMeldekortHttpKlient(
    prometheusRegistry: PrometheusRegistry = PrometheusRegistry.defaultRegistry,
    engine: HttpClientEngine = CIO.create { },
) = HttpClient(engine) {
    expectSuccess = false
    install(ContentNegotiation) {
        jackson { }
    }

    install(PrometheusMetricsPlugin) {
        this.baseName = "dp_saksbehandling_dp_meldekort_http_klient"
        this.registry = prometheusRegistry
    }
}
