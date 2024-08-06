package no.nav.dagpenger.saksbehandling.streams.skjerming

import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.streams.kafka.stringSerde
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Consumed

private val logger = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

fun StreamsBuilder.skjermetPersonStatus(
    topic: String,
    håndter: (String, Boolean) -> Unit,
): Unit =
    this
        .stream(topic, Consumed.with(stringSerde, stringSerde))
        .mapValues { _, value -> value.toBoolean() }
        .peek(loggPakke)
        .foreach(håndter)

private val loggPakke: (String, Boolean) -> Unit = { fnr, status ->
    logger.info { "Mottok melding om skjermet person" }
}
