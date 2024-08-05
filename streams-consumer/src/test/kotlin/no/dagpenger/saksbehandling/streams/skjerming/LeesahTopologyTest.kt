package no.dagpenger.saksbehandling.streams.skjerming

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.dagpenger.saksbehandling.streams.kafka.specificAvroSerde
import no.dagpenger.saksbehandling.streams.kafka.topology
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.adressebeskyttelse.Adressebeskyttelse
import no.nav.person.pdl.leesah.adressebeskyttelse.Gradering
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.TopologyTestDriver
import org.junit.jupiter.api.Test

private val logger = KotlinLogging.logger {}

fun StreamsBuilder.adressebeskyttetStream(
    topic: String,
    håndter: (String, Personhendelse) -> Unit,
): Unit =
    this.stream<String, Personhendelse>(topic)
        .peek(loggPakke)
        .foreach(håndter)

private val loggPakke: (String, Personhendelse) -> Unit = { fnr, personHendelse ->
    logger.info { "Mottok melding om skjermet person $fnr med hendelse ${personHendelse.adressebeskyttelse.gradering}" }
}
private val personhendelseSerde =
    specificAvroSerde<Personhendelse>(
        config = mapOf(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to "http://localhost:8081"),
    )

class LeesahTopologyTest {
    @Test
    fun `Skal håndtere melding på topicen`() {
        runBlocking {
            val testHandler = TestHandler()
            val inputTopic =
                TopologyTestDriver(
                    topology {
                        adressebeskyttetStream("topic", testHandler::handle)
                    },
                ).createInputTopic(
                    "topic",
                    Serdes.String().serializer(),
                    personhendelseSerde.serializer(),
                )
            val personhendelse: Personhendelse =
                Personhendelse.newBuilder().setAdressebeskyttelse(
                    Adressebeskyttelse(Gradering.STRENGT_FORTROLIG_UTLAND),
                ).build()
            inputTopic.pipeInput("123", personhendelse)
            testHandler.mutableMap shouldBe
                mapOf(
                    "123" to true,
                    "456" to false,
                )
        }
    }

    private class TestHandler {
        val mutableMap = mutableMapOf<String, Personhendelse>()

        fun handle(
            fnr: String,
            personhendelse: Personhendelse,
        ) {
            mutableMap[fnr] = personhendelse
        }
    }
}
