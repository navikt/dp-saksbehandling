package no.nav.dagpenger.saksbehandling.streams.skjerming

import no.nav.dagpenger.saksbehandling.streams.kafka.stringSerde
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Consumed

fun StreamsBuilder.skjermetPersonStatus(
    topic: String,
    håndter: (String, Boolean) -> Unit,
): Unit =
    this
        .stream(topic, Consumed.with(stringSerde, stringSerde))
        .mapValues { _, value -> value.toBoolean() }
        .foreach(håndter)
