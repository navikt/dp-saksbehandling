package no.nav.dagpenger.saksbehandling.skjerming

import mu.KotlinLogging

private val logger = KotlinLogging.logger { }
private val sikkerLogg = KotlinLogging.logger("tjenestekall")

internal class SkjermingConsumer(private val repository: SkjermingRepository) {
    fun oppdaterSkjermetStatus(
        fnr: String,
        skjermetStatus: Boolean,
    ) {
        repository.oppdaterSkjermingStatus(fnr, skjermetStatus).also {
            when (it) {
                0 -> logger.debug { "Ingen person oppdatert med ny skjerming status" }
                1 -> {
                    logger.info { "Person oppdatert med ny skjerming status" }
                    sikkerLogg.info { "Person($fnr) oppdatert med ny skjerming status($skjermetStatus)" }
                }

                else -> {
                    logger.error { "Flere enn en person oppdatert med ny skjerming status" }
                    sikkerLogg.error { "Flere enn en person($fnr) oppdatert med ny skjerming status($skjermetStatus" }
                }
            }
            logger.info { "Skjermet status for fnr=$fnr er oppdatert til $skjermetStatus" }
        }
    }
}
