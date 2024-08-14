// shamelessly copied from navikt/hm-personhendelse
package no.nav.dagpenger.saksbehandling.streams.kafka

import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology

fun topology(block: StreamsBuilder.() -> Unit): Topology = StreamsBuilder().apply(block).build()

fun kafkaStreams(
    configuration: Map<String, String>,
    block: StreamsBuilder.() -> Unit,
): KafkaStreams = KafkaStreams(topology(block), configuration.toProperties())
