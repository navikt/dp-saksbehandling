package no.nav.dagpenger.saksbehandling.streams.skjerming

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.streams.kafka.stringSerde
import no.nav.dagpenger.saksbehandling.streams.kafka.topology
import org.apache.kafka.streams.TopologyTestDriver
import org.junit.jupiter.api.Test

class SkjermetPersonStatusTopologyTest {
    @Test
    fun `Skal håndtere melding på topicen`() {
        runBlocking {
            val bubba = TestHandler()

            val inputTopic =
                TopologyTestDriver(
                    topology {
                        skjermetPersonStatus("topic", bubba::handle)
                    },
                ).createInputTopic(
                    "topic",
                    stringSerde.serializer(),
                    stringSerde.serializer(),
                )

            inputTopic.pipeInput("123", "true")
            inputTopic.pipeInput("456", "false")

            bubba.mutableMap shouldBe
                mapOf(
                    "123" to true,
                    "456" to false,
                )
        }
    }

    private class TestHandler {
        val mutableMap = mutableMapOf<String, Boolean>()

        fun handle(
            fnr: String,
            status: Boolean,
        ) {
            mutableMap[fnr] = status
        }
    }
}
