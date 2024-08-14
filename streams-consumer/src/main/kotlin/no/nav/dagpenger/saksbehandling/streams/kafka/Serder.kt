package no.nav.dagpenger.saksbehandling.streams.kafka

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes

val stringSerde: Serde<String> = Serdes.String()

fun <T : SpecificRecord> specificAvroSerde(): Serde<T> =
    SpecificAvroSerde<T>().apply {
        configure(KafkaConfiguration.kafkaSchemaRegistryConfiguration(), false)
    }
