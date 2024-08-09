package no.nav.dagpenger.saksbehandling.adressebeskyttelse

import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Counter
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.pdl.PDLKlient

private val logger = KotlinLogging.logger { }
private val sikkerLogg = KotlinLogging.logger("tjenestekall")

internal class AdressebeskyttelseConsumer(
    private val repository: AdressebeskyttelseRepository,
    private val pdlKlient: PDLKlient,
    registry: CollectorRegistry = CollectorRegistry.defaultRegistry,
) {
    private val counter: Counter = registry.lagCounter()

    fun oppdaterAdressebeskyttelseStatus(
        fnr: String,
        historiskeFnr: Set<String>,
    ) {
        runBlocking {
            val fnrs = historiskeFnr.toMutableSet() + fnr
            repository.eksistererIDPsystem(fnrs).forEach { fnr ->
                sikkerLogg.info { "$fnr eksisterer i db. Henter adressebeskyttelsestatus for personen" }
                pdlKlient.person(fnr).getOrThrow().adresseBeskyttelseGradering.let { gradering ->
                    sikkerLogg.info { "Person $fnr har adressebeskyttelsestatus: $gradering" }
                    repository.oppdaterAdressebeskyttetStatus(
                        fnr,
                        gradering,
                    )
                    logger.info { "Person oppdatert med ny gradering av adressebeskyttelsestatus" }
                    sikkerLogg.info { "Person($fnr) oppdatert med ny gradering av adressebeskyttelsestatus($gradering)" }
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
