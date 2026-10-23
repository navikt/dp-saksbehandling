package no.nav.dagpenger.saksbehandling.streams.kafka

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import org.apache.avro.specific.SpecificRecord
import org.apache.avro.util.ClassSecurityValidator
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes

private val PDL_PAKKER = listOf("no.nav.person.pdl.leesah.", "no.nav.person.pdl.aktor.")

// Avro 1.12.2 avviser klasser som ikke står i allowlisten. Erstatter systemproperty
// org.apache.avro.SERIALIZABLE_PACKAGES, og må kjøre før første Avro-deserialisering.
private fun tillatPdlKlasser() {
    ClassSecurityValidator.setGlobal(
        ClassSecurityValidator.composite(
            ClassSecurityValidator.DEFAULT,
            ClassSecurityValidator.ClassSecurityPredicate { clazz -> PDL_PAKKER.any { clazz.name.startsWith(it) } },
        ),
    )
}

val stringSerde: Serde<String> = Serdes.String()

fun <T : SpecificRecord> specificAvroSerde(): Serde<T> {
    tillatPdlKlasser()
    return SpecificAvroSerde<T>().apply {
        configure(KafkaConfiguration.kafkaSchemaRegistryConfiguration(), false)
    }
}
