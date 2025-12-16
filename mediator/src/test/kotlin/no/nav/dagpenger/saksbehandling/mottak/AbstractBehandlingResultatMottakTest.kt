package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.helper.behandlingResultatEvent
import org.junit.jupiter.api.Test
import java.util.UUID

class AbstractBehandlingResultatMottakTest {
    private val søknadId = UUIDv7.ny()
    private val behandlingId = UUIDv7.ny()
    private val ident = "12345612345"
    private val sakId = UUIDv7.ny()
    private val rapidsConnection = TestRapid()

    internal class TestBehandlingResultatMottak(
        private val eventNames: List<String>,
        rapidsConnection: RapidsConnection,
    ) : AbstractBehandlingResultatMottak(rapidsConnection) {
        override val mottakNavn: String = "TestBehandlingResultatMottak"

        override fun requiredBehandletHendelseType(): List<String> = listOf("Søknad", "Manuell", "Meldekort")

        override fun requiredEventNames(): List<String> = eventNames.ifEmpty { super.requiredEventNames() }

        private var counter = 0

        fun count() = counter

        override fun håndter(
            behandlingResultat: BehandlingResultat,
            packet: JsonMessage,
            context: MessageContext,
            metadata: MessageMetadata,
            meterRegistry: MeterRegistry,
        ) {
            counter++
        }
    }

    @Test
    fun `Skal kunne overstyre hendelse typer som håndteres`() {
        val mottak1 = TestBehandlingResultatMottak(listOf(), rapidsConnection)
        val mottak2 = TestBehandlingResultatMottak(listOf("hubba"), rapidsConnection)

        rapidsConnection.sendTestMessage(
            behandlingResultat(eventNavn = "behandlingsresultat"),
        )
        rapidsConnection.sendTestMessage(
            behandlingResultat(eventNavn = "hubba"),
        )
        rapidsConnection.sendTestMessage(
            behandlingResultat(eventNavn = "hubba"),
        )

        mottak1.count() shouldBe 1
        mottak2.count() shouldBe 2
    }

    private fun behandlingResultat(
        ident: String = this.ident,
        behandlingId: String = this.behandlingId.toString(),
        søknadId: String = this.søknadId.toString(),
        behandletHendelseType: String = "Søknad",
        harRett: Boolean = true,
        basertPå: UUID? = null,
        eventNavn: String,
    ): String =
        behandlingResultatEvent(
            ident = ident,
            behandlingId = behandlingId,
            behandletHendelseId = søknadId,
            behandletHendelseType = behandletHendelseType,
            harRett = harRett,
            basertPå = basertPå,
            eventNavn = eventNavn,
        )
}
