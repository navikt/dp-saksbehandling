package no.nav.dagpenger.saksbehandling.streams.kafka

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import org.apache.avro.specific.SpecificRecord
import org.apache.avro.util.ClassSecurityValidator
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes

private const val LEESAH_PAKKE = "no.nav.person.pdl.leesah."

// Avro 1.12.2 avviser klasser som ikke står i allowlisten. Erstatter systemproperty
// org.apache.avro.SERIALIZABLE_PACKAGES, og må kjøre før første Avro-deserialisering.
private fun tillatLeesahKlasser() {
    ClassSecurityValidator.setGlobal(
        ClassSecurityValidator.composite(
            ClassSecurityValidator.DEFAULT,
            ClassSecurityValidator.ClassSecurityPredicate { clazz -> clazz.name.startsWith(LEESAH_PAKKE) },
        ),
    )
}

val stringSerde: Serde<String> = Serdes.String()

fun <T : SpecificRecord> specificAvroSerde(): Serde<T> {
    tillatLeesahKlasser()
    return SpecificAvroSerde<T>().apply {
        configure(KafkaConfiguration.kafkaSchemaRegistryConfiguration(), false)
    }
}
