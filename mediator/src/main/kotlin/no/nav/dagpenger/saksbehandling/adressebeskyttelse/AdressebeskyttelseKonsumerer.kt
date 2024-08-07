package no.nav.dagpenger.saksbehandling.adressebeskyttelse

import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Counter
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.pdl.PDLKlient

private val logger = KotlinLogging.logger { }
private val sikkerLogg = KotlinLogging.logger("tjenestekall")

internal class AdressebeskyttelseKonsumerer(
    private val repository: AdressebeskyttelseRepository,
    private val pdlKlient: PDLKlient,
    registry: CollectorRegistry = CollectorRegistry.defaultRegistry,
) {
    private val counter: Counter = registry.lagCounter()

    fun oppdaterAdressebeskyttelseStatus(
        fnr: String,
        historiskeFnr: Set<String>,
    ) {
        val fnrs = historiskeFnr.toMutableSet() + fnr
        repository.eksistererIDPsystem(fnrs).forEach {
            runBlocking {
                pdlKlient.person(it).getOrThrow().adresseBeskyttelseGradering.let { gradering ->
                    repository.oppdaterAdressebeskyttetStatus(
                        it,
                        gradering,
                    )
                    logger.info { "Person oppdatert med ny gradering av adressebeskyttelsestatus" }
                    sikkerLogg.info { "Person($it) oppdatert med ny gradering av adressebeskyttelsestatus($gradering)" }
                    counter.inc("$gradering")
                }
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
