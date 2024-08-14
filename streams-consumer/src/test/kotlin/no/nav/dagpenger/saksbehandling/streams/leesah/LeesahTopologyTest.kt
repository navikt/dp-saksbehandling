package no.nav.dagpenger.saksbehandling.streams.leesah

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.streams.kafka.specificAvroSerde
import no.nav.dagpenger.saksbehandling.streams.kafka.stringSerde
import no.nav.dagpenger.saksbehandling.streams.kafka.topology
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.adressebeskyttelse.Adressebeskyttelse
import no.nav.person.pdl.leesah.adressebeskyttelse.Gradering.STRENGT_FORTROLIG_UTLAND
import org.apache.kafka.streams.TopologyTestDriver
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

internal class LeesahTopologyTest {
    private val personhendelseSerde = specificAvroSerde<Personhendelse>()

    @Test
    fun `Skal håndtere melding med opplysningstype adressebeskyttet på topicen`() {
        runBlocking {
            val testHandler = TestHandler()
            val topology = topology { adressebeskyttetStream("pdl.leesah-v1", testHandler::handle) }

            val inputTopic =
                TopologyTestDriver(topology).createInputTopic(
                    "pdl.leesah-v1",
                    stringSerde.serializer(),
                    personhendelseSerde.serializer(),
                )
            val ident = "123"
            val personhendelse = lagPersonhendelse(historiskeIdenter = listOf("123", "456"))
            inputTopic.pipeInput(ident, personhendelse)

            eventually(5.seconds) {
                testHandler.mutableMap shouldBe setOf("123", "456")
            }

            val ident2 = "1234"
            testHandler.reset()
            inputTopic.pipeInput(ident2, lagPersonhendelse("test", listOf("123")))

            delay(5000)
            testHandler.mutableMap shouldBe null
        }
    }

    private class TestHandler {
        var mutableMap: Set<String>? = null

        fun handle(identer: Set<String>) {
            mutableMap = identer
        }

        fun reset() {
            mutableMap = null
        }
    }

    private fun lagPersonhendelse(
        opplysningstype: String = "ADRESSEBESKYTTELSE_V1",
        historiskeIdenter: List<String>,
    ): Personhendelse? =
        Personhendelse.newBuilder()
            .setHendelseId("123")
            .setPersonidenter(historiskeIdenter)
            .setMaster("PDL")
            .setOpprettet(Instant.now())
            .setOpplysningstype(opplysningstype)
            .setEndringstype(Endringstype.ANNULLERT)
            .setAdressebeskyttelse(Adressebeskyttelse(STRENGT_FORTROLIG_UTLAND)).build()
}
