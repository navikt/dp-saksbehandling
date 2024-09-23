package no.nav.dagpenger.saksbehandling.utsending

import io.kotest.matchers.shouldBe
import io.prometheus.metrics.model.registry.PrometheusRegistry
import io.prometheus.metrics.model.snapshots.CounterSnapshot
import io.prometheus.metrics.model.snapshots.MetricSnapshot
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.utsending.hendelser.DistribuertHendelse
import org.junit.jupiter.api.Test

class UtsendingMetrikkTest {
    @Test
    fun `Sjekk om det telles når brev er distribuert`() {
        Utsending(
            oppgaveId = UUIDv7.ny(),
            ident = "12345678901",
            brev = "brev",
            tilstand = Utsending.AvventerDistribuering,
        ).mottaDistribuertKvittering(
            DistribuertHendelse(
                journalpostId = "123",
                oppgaveId = UUIDv7.ny(),
                distribusjonId = "123",
            ),
        )

        PrometheusRegistry.defaultRegistry.getSnapShot<CounterSnapshot> {
            it == "dp_saksbehandling_utsending_vedtaksbrev"
        }.let { snapshot ->
            snapshot.dataPoints.single { it.labels["type"] == "avslagMinsteinntekt" }.value shouldBe 1.0
        }
    }
}

inline fun <reified T : MetricSnapshot> PrometheusRegistry.getSnapShot(noinline metriceNamePredicate: (String) -> Boolean): T {
    return this.scrape(metriceNamePredicate).singleOrNull()?.let { it as T }
        ?: throw NoSuchElementException()
}
