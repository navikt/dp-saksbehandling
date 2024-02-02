package no.nav.dagpenger.behandling.hendelser.mottak

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

class RapidFilterTest {
    private val testRapid = TestRapid()
    private val testMessage =
        mapOf(
            "@event_name" to "manuell_behandling",
            "@id" to "id1",
            "søknad_uuid" to "søknadId33",
            "seksjon_navn" to "mulig gjenopptak",
            "identer" to "ident1",
            "fakta" to
                listOf(
                    mapOf(
                        "id" to "33",
                        "navn" to "Har brukt opp forrige dagpengeperiode",
                    ),
                ),
            "identer" to
                listOf(
                    mapOf(
                        "id" to "01010155555",
                        "type" to "folkeregisterident",
                        "historisk" to false,
                    ),
                    mapOf(
                        "id" to "1000098693185",
                        "type" to "aktørid",
                        "historisk" to false,
                    ),
                ),
        )

    @ParameterizedTest
    @CsvSource(
        VurderMinsteinntektAvslagMottak.MULIG_GJENOPPTAK + ", true",
        VurderMinsteinntektAvslagMottak.EØS_ARBEID + ", true",
        VurderMinsteinntektAvslagMottak.LUKKEDE_SAKER_NYLIG + ", true",
        VurderMinsteinntektAvslagMottak.SVANGERSKAPSRELATERTE_SYKEPENGER + ", true",
        VurderMinsteinntektAvslagMottak.INNTEKT_FRA_FANGST_OG_FISKE + ", true",
        VurderMinsteinntektAvslagMottak.INNTEKT_NESTE_KALENDERMÅNED + ", true",
        VurderMinsteinntektAvslagMottak.JOBB_UTENFOR_NORGE + ", true",
        "tull og tøys, false",
    )
    fun `Skal behandle pakker med alle required keys og gyldige seksjon_navn`(
        årsakManuellVurderingMinsteinntektAvslag: String,
        skalBehandles: Boolean,
    ) {
        val testListener = TestListener(testRapid)

        testRapid.sendTestMessage(
            testMessage.muterOgKonverterToJsonString {
                it.remove("seksjon_navn")
                it.put("seksjon_navn", årsakManuellVurderingMinsteinntektAvslag)
            },
        )
        testListener.onPacketCalled shouldBe skalBehandles
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
        lateinit var packet: JsonMessage

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
            this.packet = packet
        }

        override fun onError(
            problems: MessageProblems,
            context: MessageContext,
        ) {
            println(problems.toExtendedReport())
        }
    }
}
