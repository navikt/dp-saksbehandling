package no.nav.dagpenger.saksbehandling.mottak

import io.kotest.matchers.shouldBe
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class RapidFilterBehandlingOpprettetMottakTest {
    private val testRapid = TestRapid()
    private val testMessage =
        mapOf(
            "@event_name" to "behandling_opprettet",
            "@id" to "id1",
            "@opprettet" to "2024-02-27T10:41:52.800935377",
            "søknadId" to "søknadId33",
            "behandlingId" to "behandlingId4949494",
            "ident" to "ident123",
        )

    @Test
    fun `Skal behandle pakker med alle required keys`() {
        val testListener = TestListener(testRapid)

        testRapid.sendTestMessage(JsonMessage.newMessage(testMessage).toJson())
        testListener.onPacketCalled shouldBe true
    }

    @ParameterizedTest
    @CsvSource(
        "søknadId, false",
        "behandlingId, false",
        "ident, false",
    )
    fun `Skal ikke behandle pakker som mangler required keys`(
        requiredKey: String,
        testResult: Boolean,
    ) {
        val testListener = TestListener(testRapid)
        val mutertTestMessage = JsonMessage.newMessage(testMessage.muterOgKonverterToJsonString { it.remove(requiredKey) }).toJson()
        testRapid.sendTestMessage(mutertTestMessage)
        testListener.onPacketCalled shouldBe testResult
    }

    private fun Map<String, Any>.muterOgKonverterToJsonString(block: (map: MutableMap<String, Any>) -> Unit): String {
        val mutableMap = this.toMutableMap()
        block.invoke(mutableMap)
        return JsonMessage.newMessage(mutableMap).toJson()
    }

    private class TestListener(rapidsConnection: RapidsConnection) : River.PacketListener {
        var onPacketCalled = false
        lateinit var packet: JsonMessage

        init {
            River(rapidsConnection).apply(
                BehandlingOpprettetMottak.rapidFilter,
            ).register(this)
        }

        override fun onPacket(
            packet: JsonMessage,
            context: MessageContext,
        ) {
            this.onPacketCalled = true
            this.packet = packet
        }

        override fun onError(
            problems: MessageProblems,
            context: MessageContext,
        ) {
            println(problems.toExtendedReport())
        }

        override fun onSevere(
            error: MessageProblems.MessageException,
            context: MessageContext,
        ) {
            println(error.problems.toExtendedReport())
        }
    }
}
