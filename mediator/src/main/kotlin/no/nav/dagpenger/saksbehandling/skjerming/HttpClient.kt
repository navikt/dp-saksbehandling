package no.nav.dagpenger.saksbehandling.skjerming

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.prometheus.metrics.model.registry.PrometheusRegistry
import no.nav.dagpenger.ktor.client.metrics.PrometheusMetricsPlugin

fun createHttpClient(
    prometheusRegistry: PrometheusRegistry = PrometheusRegistry.defaultRegistry,
    metricsBaseName: String,
    engine: HttpClientEngine,
    expectSuccess: Boolean = true,
) = HttpClient(engine) {
    this.expectSuccess = expectSuccess
    install(PrometheusMetricsPlugin) {
        this.baseName = metricsBaseName
        this.registry = prometheusRegistry
    }
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }
    }
}
