package no.nav.dagpenger.saksbehandling.mottak

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.helper.vedtakFattetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class VedtakFattetMottakTest {
    private val testIdent = "12345678901"
    private val søknadId = UUID.randomUUID()
    private val behandlingId = UUID.randomUUID()
    private val sak = Sak("12342", "Arena")
    private val opprettet = LocalDateTime.parse("2024-02-27T10:41:52.800935377")
    private val oppgave =
        Oppgave(
            oppgaveId = UUIDv7.ny(),
            ident = testIdent,
            behandlingId = behandlingId,
            opprettet = opprettet,
            behandling =
                Behandling(
                    behandlingId = behandlingId,
                    person = Person(id = UUIDv7.ny(), ident = testIdent),
                    opprettet = opprettet,
                ),
        )

    private val testRapid = TestRapid()
    private val oppgaveMediatorMock = mockk<OppgaveMediator>(relaxed = true)

    init {
        VedtakFattetMottak(testRapid, oppgaveMediatorMock)
    }

    @Test
    fun `Skal behandle vedtak fattet hendelse`() {
        every { oppgaveMediatorMock.ferdigstillOppgave(any()) } returns oppgave
        testRapid.sendTestMessage(
            vedtakFattetHendelse(
                ident = testIdent,
                søknadId = søknadId,
                behandlingId = behandlingId,
                sakId = sak.id.toInt(),
            ),
        )

        val vedtakFattetHendelse =
            VedtakFattetHendelse(
                behandlingId = behandlingId,
                søknadId = søknadId,
                ident = testIdent,
                sak = sak,
            )
        verify(exactly = 1) {
            oppgaveMediatorMock.ferdigstillOppgave(vedtakFattetHendelse)
        }
        testRapid.inspektør.size shouldBe 1
        testRapid.inspektør.message(0).let { jsonNode ->
            jsonNode.path("@event_name").asText() shouldBe "start_utsending"
            jsonNode["oppgaveId"].asUUID() shouldBe oppgave.oppgaveId
            jsonNode["ident"].asText() shouldBe testIdent
            jsonNode["behandlingId"].asUUID() shouldBe behandlingId
            jsonNode["sak"].let { sakIdNode ->
                sakIdNode["id"].asText() shouldBe sak.id
                sakIdNode["kontekst"].asText() shouldBe sak.kontekst
            }
        }
    }
}
