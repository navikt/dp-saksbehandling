package no.nav.dagpenger.saksbehandling.adressebeskyttelse

import io.github.oshai.kotlinlogging.KotlinLogging
import io.prometheus.metrics.core.metrics.Counter
import io.prometheus.metrics.model.registry.PrometheusRegistry
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.pdl.PDLKlient

private val logger = KotlinLogging.logger {}

internal class AdressebeskyttelseConsumer(
    private val repository: AdressebeskyttelseRepository,
    private val pdlKlient: PDLKlient,
    registry: PrometheusRegistry = PrometheusRegistry.defaultRegistry,
) {
    private val counter: Counter = registry.lagCounter()

    fun oppdaterAdressebeskyttelseGradering(identer: Set<String>) {
        val eksistererIDPsystem = repository.eksistererIDPsystem(identer)
        logger.info {
            "Oppdaterer adressebeskyttelse gradering for ${eksistererIDPsystem.size} personer av totalt ${identer.size} mottatte identer."
        }

        eksistererIDPsystem.forEach { ident ->
            val adresseBeskyttelseGradering =
                runBlocking {
                    pdlKlient.person(ident).getOrThrow().adresseBeskyttelseGradering
                }

            adresseBeskyttelseGradering.let { gradering: AdressebeskyttelseGradering ->
                repository.oppdaterAdressebeskyttelseGradering(ident, gradering)
                counter.inc("$gradering")
            }
        }
    }

    private fun Counter.inc(status: String) = this.labelValues(status).inc()

    private fun PrometheusRegistry.lagCounter(): Counter =
        Counter
            .builder()
            .name("dp_saksbehandling_adressebeskyttelse_oppdateringer")
            .labelNames("status")
            .help("Antall oppdateringer av adressebeskyttelsestatus")
            .register(this)
}
