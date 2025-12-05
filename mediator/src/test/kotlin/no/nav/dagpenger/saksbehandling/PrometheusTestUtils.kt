package no.nav.dagpenger.saksbehandling

import io.prometheus.metrics.model.registry.PrometheusRegistry
import io.prometheus.metrics.model.snapshots.MetricSnapshot

inline fun <reified T : MetricSnapshot> PrometheusRegistry.getSnapShot(noinline metriceNamePredicate: (String) -> Boolean): T =
    this.scrape(metriceNamePredicate).singleOrNull()?.let { it as T }
        ?: throw NoSuchElementException()
