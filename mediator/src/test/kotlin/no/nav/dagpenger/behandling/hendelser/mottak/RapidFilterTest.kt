package no.nav.dagpenger.behandling.hendelser.mottak

import io.kotest.matchers.shouldBe
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test

class RapidFilterTest {
    private val testRapid = TestRapid()
    private val testMessage =
        mapOf(
            "@event_name" to "manuell_behandling",
            "@id" to "id1",
            "søknad_uuid" to "søknadId33",
            "seksjon_navn" to "mulig gjenopptak",
            "identer" to "ident1",
        )

    @Test
    fun `Skal behandle pakker med alle required keys og gyldige seksjon_navn`() {
        val testListener = TestListener(testRapid)
        testRapid.sendTestMessage(
            JsonMessage.newMessage(testMessage).toJson(),
        )
        testListener.onPacketCalled shouldBe true

        testRapid.sendTestMessage(
            testMessage.muterOgKonverterToJsonString {
                it.remove("seksjon_navn")
                it.put("seksjon_navn", "svangerskapsrelaterte sykepenger")
            },
        )
        testListener.onPacketCalled shouldBe true

        testRapid.sendTestMessage(
            testMessage.muterOgKonverterToJsonString {
                it.remove("seksjon_navn")
                it.put("seksjon_navn", "EØS-arbeid")
            },
        )
        testListener.onPacketCalled shouldBe true

        testRapid.sendTestMessage(
            testMessage.muterOgKonverterToJsonString {
                it.remove("seksjon_navn")
                it.put("seksjon_navn", "har hatt lukkede saker siste 8 uker")
            },
        )
        testListener.onPacketCalled shouldBe true

        testRapid.sendTestMessage(
            testMessage.muterOgKonverterToJsonString {
                it.remove("seksjon_navn")
                it.put("seksjon_navn", "det er inntekt neste kalendermåned")
            },
        )
        testListener.onPacketCalled shouldBe true

        testRapid.sendTestMessage(
            testMessage.muterOgKonverterToJsonString {
                it.remove("seksjon_navn")
                it.put("seksjon_navn", "mulige inntekter fra fangst og fisk")
            },
        )
        testListener.onPacketCalled shouldBe true

        testRapid.sendTestMessage(
            testMessage.muterOgKonverterToJsonString {
                it.remove("seksjon_navn")
                it.put("seksjon_navn", "jobbet utenfor Norge")
            },
        )
        testListener.onPacketCalled shouldBe true
    }

    @Test
    fun `Skal ikke behandle pakker med ugyldig seksjon_navn`() {
        val testListener = TestListener(testRapid)

        testRapid.sendTestMessage(
            testMessage.muterOgKonverterToJsonString {
                it.remove("seksjon_navn")
                it.put("seksjon_navn", "tull og tøys")
            },
        )

        testListener.onPacketCalled shouldBe false
    }

    @Test
    fun `Skal ikke behandle pakker med manglende required keys`() {
        val testListener = TestListener(testRapid)

        testRapid.sendTestMessage(
            testMessage.muterOgKonverterToJsonString { it.remove("søknad_uuid") },
        )

        testListener.onPacketCalled shouldBe false

        testRapid.sendTestMessage(
            testMessage.muterOgKonverterToJsonString { it.remove("seksjon_navn") },
        )

        testListener.onPacketCalled shouldBe false

        testRapid.sendTestMessage(
            testMessage.muterOgKonverterToJsonString { it.remove("identer") },
        )

        testListener.onPacketCalled shouldBe false

// TODO fungerer ikke
//        testRapid.sendTestMessage(
//            testMessage.muterOgKonverterToJsonString { it.remove("@id") },
//        )
//
//        testListener.onPacketCalled shouldBe false
    }

    private fun Map<String, Any>.muterOgKonverterToJsonString(block: (map: MutableMap<String, Any>) -> Unit): String {
        val mutableMap = this.toMutableMap()
        block.invoke(mutableMap)
        return JsonMessage.newMessage(mutableMap).toJson()
    }

    private class TestListener(rapidsConnection: RapidsConnection) : River.PacketListener {
        var onPacketCalled = false

        init {
            River(rapidsConnection).apply(
                VurderMinsteinntektAvslagMottak.rapidFilter,
            ).register(this)
        }

        override fun onPacket(
            packet: JsonMessage,
            context: MessageContext,
        ) {
            this.onPacketCalled = true
        }

        override fun onError(
            problems: MessageProblems,
            context: MessageContext,
        ) {
            println(problems.toExtendedReport())
        }
    }
}
