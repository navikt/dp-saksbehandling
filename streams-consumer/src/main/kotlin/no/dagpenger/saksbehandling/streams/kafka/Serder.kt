package no.dagpenger.saksbehandling.streams.kafka

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes

val stringSerde: Serde<String> = Serdes.String()

fun <T : SpecificRecord> specificAvroSerde(config: Map<String, Any> = KafkaConfiguration.kafkaSchemaRegistryConfiguration()): Serde<T> =
    SpecificAvroSerde<T>().apply {
        configure(config, false)
    }
