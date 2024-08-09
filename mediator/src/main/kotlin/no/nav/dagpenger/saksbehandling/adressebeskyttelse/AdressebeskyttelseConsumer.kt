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
        logger.info { "Starter oppdatering avdressebeskyttelse status" }
        val fnrs = historiskeFnr.toMutableSet() + fnr
        logger.info { "Sjekker om $fnrs eksisterer i db" }
        repository.eksistererIDPsystem(fnrs).forEach { fnr ->
            logger.info { "$fnr eksisterer i db. Henter adressebeskyttelsestatus for personen" }
            val adresseBeskyttelseGradering = runBlocking {
                pdlKlient.person(fnr).getOrThrow().adresseBeskyttelseGradering
            }

            adresseBeskyttelseGradering.let { gradering ->
                logger.info { "Person $fnr har adressebeskyttelsestatus: $gradering" }
                repository.oppdaterAdressebeskyttetStatus(
                    fnr,
                    gradering,
                )
                logger.info { "Person oppdatert med ny gradering av adressebeskyttelsestatus" }
                logger.info { "Person($fnr) oppdatert med ny gradering av adressebeskyttelsestatus($gradering)" }
                counter.inc("$gradering")
            }
            sikkerLogg.info { "Ferdig med å oppdatere adressebeskyttelsestatus" }
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
