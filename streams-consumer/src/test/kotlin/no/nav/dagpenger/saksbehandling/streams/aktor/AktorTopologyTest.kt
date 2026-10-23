package no.nav.dagpenger.saksbehandling.streams.aktor

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.streams.kafka.specificAvroSerde
import no.nav.dagpenger.saksbehandling.streams.kafka.topology
import no.nav.person.pdl.aktor.v2.Aktor
import no.nav.person.pdl.aktor.v2.Identifikator
import no.nav.person.pdl.aktor.v2.Type
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.TopologyTestDriver
import org.junit.jupiter.api.Test

class AktorTopologyTest {
    private val topic = "pdl.aktor-v2"
    private val mottatt = mutableListOf<Set<String>>()
    private val inputTopic =
        TopologyTestDriver(topology { identendringStream(topic) { mottatt.add(it) } })
            .createInputTopic(topic, Serdes.ByteArray().serializer(), specificAvroSerde<Aktor>().serializer())

    @Test
    fun `gir videre historiske folkeregisteridenter og NPID, ikke aktørId eller gjeldende ident`() {
        inputTopic.pipeInput(
            "aktor".toByteArray(),
            aktor(
                Identifikator("11111111111", Type.FOLKEREGISTERIDENT, true),
                Identifikator("41111111111", Type.FOLKEREGISTERIDENT, false),
                Identifikator("22222222222", Type.NPID, false),
                Identifikator("1000000000001", Type.AKTORID, false),
                Identifikator("1000000000002", Type.AKTORID, true),
            ),
        )

        mottatt shouldBe listOf(setOf("41111111111", "22222222222"))
    }

    @Test
    fun `ignorerer personer uten historiske identer`() {
        inputTopic.pipeInput(
            "aktor".toByteArray(),
            aktor(
                Identifikator("11111111111", Type.FOLKEREGISTERIDENT, true),
                Identifikator("1000000000001", Type.AKTORID, false),
            ),
        )

        mottatt shouldBe emptyList()
    }

    @Test
    fun `ignorerer tombstones`() {
        inputTopic.pipeInput("aktor".toByteArray(), null)

        mottatt shouldBe emptyList()
    }

    private fun aktor(vararg identifikatorer: Identifikator) = Aktor(identifikatorer.toList())
}
