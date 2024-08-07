package no.nav.dagpenger.saksbehandling.streams.leesah

import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.streams.kafka.specificAvroSerde
import no.nav.dagpenger.saksbehandling.streams.kafka.stringSerde
import no.nav.person.pdl.leesah.Personhendelse
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Consumed

private val logger = KotlinLogging.logger {}

fun StreamsBuilder.adressebeskyttetStream(
    topic: String,
    håndter: (String, Personhendelse) -> Unit,
): Unit =
    this.stream(topic, Consumed.with(stringSerde, specificAvroSerde<Personhendelse>()))
        .peek(loggPakke)
        .filter { _, personhendelse -> personhendelse.opplysningstype == "ADRESSEBESKYTTELSE_V1" }
        .foreach(håndter)

private val loggPakke: (String, Personhendelse) -> Unit = { fnr, personHendelse ->
    logger.info { "Mottok melding om skjermet person $fnr med hendelse ${personHendelse.adressebeskyttelse.gradering}" }
}
