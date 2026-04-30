package no.nav.dagpenger.saksbehandling.skjerming

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.prometheus.metrics.model.registry.PrometheusRegistry
import no.nav.dagpenger.ktor.client.metrics.PrometheusMetricsPlugin
import no.nav.dagpenger.saksbehandling.serder.applyDefault

fun createHttpClient(
    prometheusRegistry: PrometheusRegistry = PrometheusRegistry.defaultRegistry,
    metricsBaseName: String,
    engine: HttpClientEngine,
    expectSuccess: Boolean = true,
    configure: HttpClientConfig<*>.() -> Unit = {},
) = HttpClient(engine) {
    this.expectSuccess = expectSuccess
    install(PrometheusMetricsPlugin) {
        this.baseName = metricsBaseName
        this.registry = prometheusRegistry
    }
    install(ContentNegotiation) {
        jackson { applyDefault() }
    }
    configure()
}
