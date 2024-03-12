package no.nav.dagpenger.saksbehandling.mottak

import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.Mediator
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

class BehandligOpprettetMottakTest {
    private val testRapid = TestRapid()
    private val mediator = mockk<Mediator>(relaxed = true)
    val søknadId = UUID.randomUUID()
    val behandlingId = UUID.randomUUID()
    val opprettet = LocalDateTime.parse("2024-02-27T10:41:52.800935377")
    val ident = "12345678901"
    private val søknadsbehandlingOpprettetHendelse =
        SøknadsbehandlingOpprettetHendelse(
            søknadId = søknadId,
            behandlingId = behandlingId,
            ident = ident,
            opprettet = ZonedDateTime.of(opprettet, ZoneId.systemDefault()),
        )

    init {
        BehandlingOpprettetMottak(testRapid, mediator)
    }

    @Test
    fun `Skal behandle behandling_opprettet hendelse`() {
        testRapid.sendTestMessage(behandlingOpprettetMelding)
        verify(exactly = 1) {
            mediator.behandle(søknadsbehandlingOpprettetHendelse)
        }
    }

    @Test
    fun `Skal ignorere duplikate behandling_opprettet hendelser`() {
        testRapid.sendTestMessage(behandlingOpprettetMelding)
        verify(exactly = 1) {
            mediator.behandle(søknadsbehandlingOpprettetHendelse)
            mediator.behandle(søknadsbehandlingOpprettetHendelse)
        }
    }

    @Language("JSON")
    private val behandlingOpprettetMelding =
        """
        {
            "@event_name": "behandling_opprettet",
            "@opprettet": "$opprettet",
            "@id": "9fca5cad-d6fa-4296-a057-1c5bb04cdaac",
            "søknadId": "$søknadId",
            "behandlingId": "$behandlingId",
            "ident": "$ident"
        }
        """
}
