package no.nav.dagpenger.saksbehandling.streams.leesah

import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.streams.kafka.specificAvroSerde
import no.nav.dagpenger.saksbehandling.streams.kafka.stringSerde
import no.nav.person.pdl.leesah.Personhendelse
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Consumed

private val sikkerLogger = KotlinLogging.logger { "tjenestekall" }

fun StreamsBuilder.adressebeskyttetStream(
    topic: String,
    håndter: (String, Set<String>) -> Unit,
): Unit =
    this.stream(topic, Consumed.with(stringSerde, specificAvroSerde<Personhendelse>()))
        .filter { _, personhendelse -> personhendelse.opplysningstype == "ADRESSEBESKYTTELSE_V1" }
        .peek { _, personhendelse -> sikkerLogger.debug { "Personhendelse: $personhendelse" } }
        .mapValues { _, personhendelse -> personhendelse.personidenter.toSet() }
        .foreach(håndter)
