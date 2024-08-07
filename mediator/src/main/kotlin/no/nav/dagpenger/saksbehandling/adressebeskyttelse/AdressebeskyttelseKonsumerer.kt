package no.nav.dagpenger.saksbehandling.adressebeskyttelse

import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Counter
import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.AdresseBeskyttelseGradering

private val logger = KotlinLogging.logger { }
private val sikkerLogg = KotlinLogging.logger("tjenestekall")

internal class AdressebeskyttelseKonsumerer(
    private val repository: AdressebeskyttelseRepository,
    registry: CollectorRegistry = CollectorRegistry.defaultRegistry,
) {
    private val counter: Counter = registry.lagCounter()

    fun oppdaterAdressebeskyttelseStatus(
        fnr: String,
        gradering: AdresseBeskyttelseGradering,
    ) {
        repository.oppdaterAdressebeskyttetStatus(fnr, gradering).also { raderOppdatert ->
            when (raderOppdatert) {
                0 -> {
                    logger.debug { "Ingen person oppdatert med ny gradering av adressebeskyttelsestatus" }
                    counter.inc("Ukjent")
                }

                1 -> {
                    logger.info { "Person oppdatert med ny gradering av adressebeskyttelsestatus" }
                    sikkerLogg.info { "Person($fnr) oppdatert med ny gradering av adressebeskyttelsestatus($gradering)" }
                    counter.inc("$gradering")
                }

                else -> {
                    logger.error { "Flere enn en person oppdatert med ny gradering av adressebeskyttelsestatus" }
                    sikkerLogg.error { "Flere enn en person($fnr) oppdatert med ny gradering av adressebeskyttelsestatus($gradering" }
                    counter.inc("feil")
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
