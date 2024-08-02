package no.dagpenger.saksbehandling.streams.kafka

import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes

val stringSerde: Serde<String> = Serdes.String()

