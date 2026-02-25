package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.helper.behandlingsresultatEvent
import org.junit.jupiter.api.Test
import java.util.UUID

class AbstractBehandlingsresultatMottakTest {
    private val søknadId = UUIDv7.ny()
    private val behandlingId = UUIDv7.ny()
    private val ident = "12345612345"
    private val rapidsConnection = TestRapid()

    internal class TestBehandlingsresultatMottak(
        private val eventNames: List<String>,
        private val requireAutomatiskVerdi: Boolean? = null,
        rapidsConnection: RapidsConnection,
    ) : AbstractBehandlingsresultatMottak(rapidsConnection) {
        override val mottakNavn: String = "TestBehandlingsresultatMottak"

        override fun requiredBehandletHendelseType(): List<String> = listOf("Søknad", "Manuell", "Meldekort")

        override fun requiredEventNames(): List<String> = eventNames.ifEmpty { super.requiredEventNames() }

        override fun JsonMessage.valideringsregler() {
            when (requireAutomatiskVerdi) {
                true -> this.requireValue("automatisk", true)
                false -> this.requireValue("automatisk", false)
                null -> Unit
            }
        }

        private var counter = 0

        fun count() = counter

        override fun håndter(
            behandlingsresultat: Behandlingsresultat,
            packet: JsonMessage,
            context: MessageContext,
            metadata: MessageMetadata,
            meterRegistry: MeterRegistry,
        ) {
            counter++
        }
    }

    @Test
    fun `Skal kunne overstyre hvilke hendelsetyper som håndteres`() {
        val mottak1 = TestBehandlingsresultatMottak(eventNames = listOf(), rapidsConnection = rapidsConnection)
        val mottak2 = TestBehandlingsresultatMottak(eventNames = listOf("hubba"), rapidsConnection = rapidsConnection)

        rapidsConnection.sendTestMessage(
            behandlingsresultatJson(eventNavn = "behandlingsresultat"),
        )
        rapidsConnection.sendTestMessage(
            behandlingsresultatJson(eventNavn = "hubba"),
        )
        rapidsConnection.sendTestMessage(
            behandlingsresultatJson(eventNavn = "hubba"),
        )

        mottak1.count() shouldBe 1
        mottak2.count() shouldBe 2
    }

    @Test
    fun `Skal kunne overstyre valideringsregler i rapidfilteret`() {
        val mottak1 =
            TestBehandlingsresultatMottak(eventNames = listOf(), rapidsConnection = rapidsConnection, requireAutomatiskVerdi = true)
        val mottak2 =
            TestBehandlingsresultatMottak(eventNames = listOf(), rapidsConnection = rapidsConnection, requireAutomatiskVerdi = false)

        rapidsConnection.sendTestMessage(
            behandlingsresultatJson(eventNavn = "behandlingsresultat", automatiskBehandling = true),
        )
        rapidsConnection.sendTestMessage(
            behandlingsresultatJson(eventNavn = "behandlingsresultat", automatiskBehandling = true),
        )
        rapidsConnection.sendTestMessage(
            behandlingsresultatJson(eventNavn = "behandlingsresultat", automatiskBehandling = false),
        )

        mottak1.count() shouldBe 2
        mottak2.count() shouldBe 1
    }

    private fun behandlingsresultatJson(
        ident: String = this.ident,
        behandlingId: String = this.behandlingId.toString(),
        søknadId: String = this.søknadId.toString(),
        behandletHendelseType: String = "Søknad",
        harRett: Boolean = true,
        basertPå: UUID? = null,
        automatiskBehandling: Boolean = false,
        eventNavn: String,
    ): String =
        behandlingsresultatEvent(
            ident = ident,
            behandlingId = behandlingId,
            behandletHendelseId = søknadId,
            behandletHendelseType = behandletHendelseType,
            harRett = harRett,
            basertPå = basertPå,
            automatiskBehandling = automatiskBehandling,
            eventNavn = eventNavn,
        )
}
