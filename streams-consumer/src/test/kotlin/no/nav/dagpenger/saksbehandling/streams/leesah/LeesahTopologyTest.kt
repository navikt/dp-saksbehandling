package no.nav.dagpenger.saksbehandling.streams.leesah

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.shouldBe
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
    fun `Skal håndtere melding på topicen`() {
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
            val personhendelse = lagPersonhendelse()
            inputTopic.pipeInput(ident, personhendelse)

            eventually(5.seconds) {
                testHandler.mutableMap[ident] shouldBe personhendelse
            }
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

    private fun lagPersonhendelse(): Personhendelse? =
        Personhendelse.newBuilder()
            .setHendelseId("123")
            .setPersonidenter(listOf("123"))
            .setMaster("PDL")
            .setOpprettet(Instant.now())
            .setOpplysningstype("ADRESSEBESKYTTELSE_V1")
            .setEndringstype(Endringstype.ANNULLERT)
            .setAdressebeskyttelse(Adressebeskyttelse(STRENGT_FORTROLIG_UTLAND)).build()
}
