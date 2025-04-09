package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.MeterRegistry
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
            "behandletHendelse" to
                mapOf(
                    "datatype" to "UUID",
                    "id" to "søknadId33",
                    "type" to "Søknad",
                ),
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
        "behandletHendelse, false",
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
            metadata: MessageMetadata,
            meterRegistry: MeterRegistry,
        ) {
            this.onPacketCalled = true
            this.packet = packet
        }
    }
}
