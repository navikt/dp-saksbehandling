package no.nav.dagpenger.saksbehandling.adressebeskyttelse

import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Counter
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.pdl.PDLKlient

internal class AdressebeskyttelseConsumer(
    private val repository: AdressebeskyttelseRepository,
    private val pdlKlient: PDLKlient,
    registry: CollectorRegistry = CollectorRegistry.defaultRegistry,
) {
    private val counter: Counter = registry.lagCounter()

    fun oppdaterAdressebeskyttelseStatus(identer: Set<String>) {
        repository.eksistererIDPsystem(identer).forEach { ident ->
            val adresseBeskyttelseGradering =
                runBlocking {
                    pdlKlient.person(ident).getOrThrow().adresseBeskyttelseGradering
                }

            adresseBeskyttelseGradering.let { gradering ->
                repository.oppdaterAdressebeskyttetStatus(ident, gradering)
                counter.inc("$gradering")
            }
        }
    }

    private fun Counter.inc(status: String) = this.labels(status).inc()

    private fun CollectorRegistry.lagCounter(): Counter =
        Counter.build()
            .namespace("dagpenger")
            .name("dp_saksbehandling_adressebeskyttelse_oppdateringer")
            .labelNames("status")
            .help("Antall oppdateringer av adressebeskyttelsestatus")
            .register(this)
}
