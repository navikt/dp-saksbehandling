package no.nav.dagpenger.saksbehandling.streams.leesah

import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.streams.kafka.specificAvroSerde
import no.nav.dagpenger.saksbehandling.streams.kafka.stringSerde
import no.nav.person.pdl.leesah.Personhendelse
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Consumed

private val sikkerLogger = KotlinLogging.logger("tjenestekall")
private val logger = KotlinLogging.logger { }

fun StreamsBuilder.adressebeskyttetStream(
    topic: String,
    håndter: (Set<String>) -> Unit,
): Unit =
    this.stream(topic, Consumed.with(stringSerde, specificAvroSerde<Personhendelse>()))
        .filter { _, personhendelse -> personhendelse.opplysningstype == "ADRESSEBESKYTTELSE_V1" }
        .peek { _, personhendelse ->
            sikkerLogger.info { "Personhendelse: $personhendelse" }
            logger.info { "Mottok personhendelse til prosessering, ${personhendelse.opplysningstype}" }
        }
        .mapValues { _, personhendelse -> personhendelse.personidenter.toSet() }
        .foreach { _, personidenter -> håndter(personidenter) }
