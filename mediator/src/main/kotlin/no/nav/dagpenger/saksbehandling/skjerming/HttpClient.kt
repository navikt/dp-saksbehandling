package no.nav.dagpenger.saksbehandling.skjerming

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.prometheus.client.CollectorRegistry
import no.nav.dagpenger.ktor.client.metrics.PrometheusMetricsPlugin

fun createHttpClient(
    collectorRegistry: CollectorRegistry = CollectorRegistry.defaultRegistry,
    engine: HttpClientEngine,
) = HttpClient(engine) {
    expectSuccess = true

    install(PrometheusMetricsPlugin) {
        this.baseName = "dp_saksbehandling_skjerming_http_klient"
        this.registry = collectorRegistry
    }
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }
    }
}
