package no.nav.dagpenger.saksbehandling.streams.aktor

import no.nav.dagpenger.saksbehandling.streams.kafka.specificAvroSerde
import no.nav.person.pdl.aktor.v2.Aktor
import no.nav.person.pdl.aktor.v2.Type
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.AutoOffsetReset
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Consumed

private val personidentTyper = setOf(Type.FOLKEREGISTERIDENT, Type.NPID)

/**
 * Leser pdl.aktor-v2 og gir videre identer personen ikke lenger bruker (gjeldende=false).
 * Topicet er compacted med hele befolkningen, så vi starter på latest for å bare få nye endringer.
 */
fun StreamsBuilder.identendringStream(
    topic: String,
    håndter: (historiskeIdenter: Set<String>) -> Unit,
): Unit =
    this
        .stream(
            topic,
            Consumed
                .with(Serdes.ByteArray(), specificAvroSerde<Aktor>())
                .withOffsetResetPolicy(AutoOffsetReset.latest()),
        ).filter { _, aktor -> aktor != null }
        .mapValues { _, aktor ->
            aktor.identifikatorer
                .filter { !it.gjeldende && it.type in personidentTyper }
                .map { it.idnummer }
                .toSet()
        }.filter { _, historiskeIdenter -> historiskeIdenter.isNotEmpty() }
        .foreach { _, historiskeIdenter -> håndter(historiskeIdenter) }
