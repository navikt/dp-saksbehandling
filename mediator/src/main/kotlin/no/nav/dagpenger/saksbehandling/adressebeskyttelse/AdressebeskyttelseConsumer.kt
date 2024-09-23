package no.nav.dagpenger.saksbehandling.adressebeskyttelse

import io.prometheus.metrics.core.metrics.Counter
import io.prometheus.metrics.model.registry.PrometheusRegistry
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.pdl.PDLKlient

internal class AdressebeskyttelseConsumer(
    private val repository: AdressebeskyttelseRepository,
    private val pdlKlient: PDLKlient,
    registry: PrometheusRegistry = PrometheusRegistry.defaultRegistry,
) {
    private val counter: Counter = registry.lagCounter()

    fun oppdaterAdressebeskyttelseStatus(identer: Set<String>) {
        repository.eksistererIDPsystem(identer).forEach { ident ->
            val adresseBeskyttelseGradering =
                runBlocking {
                    pdlKlient.person(ident).getOrThrow().adresseBeskyttelseGradering
                }

            adresseBeskyttelseGradering.let { gradering: AdressebeskyttelseGradering ->
                repository.oppdaterAdressebeskyttetStatus(ident, gradering)
                counter.inc("$gradering")
            }
        }
    }

    private fun Counter.inc(status: String) = this.labelValues(status).inc()

    private fun PrometheusRegistry.lagCounter(): Counter =
        Counter.builder()
            .name("dp_saksbehandling_adressebeskyttelse_oppdateringer")
            .labelNames("status")
            .help("Antall oppdateringer av adressebeskyttelsestatus")
            .register(this)
}
