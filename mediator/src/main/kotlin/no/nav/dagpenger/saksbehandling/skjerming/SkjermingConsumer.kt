package no.nav.dagpenger.saksbehandling.skjerming

import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Counter
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }
private val sikkerLogg = KotlinLogging.logger("tjenestekall")

internal class SkjermingConsumer(
    private val repository: SkjermingRepository,
    registry: CollectorRegistry = CollectorRegistry.defaultRegistry,
) {
    private val counter: Counter = registry.lagCounter()

    fun oppdaterSkjermetStatus(
        fnr: String,
        skjermetStatus: Boolean,
    ) {
        repository.oppdaterSkjermingStatus(fnr, skjermetStatus).also {
            when (it) {
                0 -> {
                    logger.debug { "Ingen person oppdatert med ny skjerming status" }
                    counter.inc("Ukjent")
                }

                1 -> {
                    logger.info { "Person oppdatert med ny skjerming status" }
                    sikkerLogg.info { "Person($fnr) oppdatert med ny skjerming status($skjermetStatus)" }
                    counter.inc("$skjermetStatus")
                }

                else -> {
                    logger.error { "Flere enn en person oppdatert med ny skjerming status" }
                    sikkerLogg.error { "Flere enn en person($fnr) oppdatert med ny skjerming status($skjermetStatus" }
                    counter.inc("feil")
                }
            }
        }
    }

    private fun Counter.inc(status: String) = this.labels(status).inc()

    private fun CollectorRegistry.lagCounter(): Counter =
        Counter
            .build()
            .namespace("dagpenger")
            .name("dp_saksbehandling_skjerming_oppdateringer")
            .labelNames("status")
            .help("Antall oppdateringer av skjerming status")
            .register(this)
}
