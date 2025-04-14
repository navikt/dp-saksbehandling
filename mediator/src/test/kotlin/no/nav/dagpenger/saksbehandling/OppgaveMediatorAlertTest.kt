package no.nav.dagpenger.saksbehandling

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.dagpenger.saksbehandling.db.person.PersonRepository
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import org.junit.jupiter.api.Test
import java.util.UUID

class OppgaveMediatorAlertTest {
    private val rapid = TestRapid()

    @Test
    fun `Skal sende alert på rapid dersom oppgave ikke finnes`() {
        val behandlingId = UUIDv7.ny()
        OppgaveMediator(
            personRepository = mockk<PersonRepository>(),
            oppgaveRepository =
                mockk<OppgaveRepository>().also {
                    every { it.finnOppgaveFor(any()) } returns null
                },
            oppslag = mockk(),
            behandlingKlient = mockk(),
            utsendingMediator = mockk(),
            meldingOmVedtakKlient = mockk(),
        ).also { it.setRapidsConnection(rapid) }.let { oppgaveMediator ->

            oppgaveMediator.settOppgaveKlarTilBehandling(
                ForslagTilVedtakHendelse(
                    ident = "12345678910",
                    søknadId = UUIDv7.ny(),
                    behandlingId = behandlingId,
                    emneknagger = emptySet(),
                ),
            )
            rapid.inspektør.size shouldBe 1
            rapid.inspektør.message(0).forventetAlert(behandlingId) shouldBe true
        }
    }

    private fun JsonNode.forventetAlert(behandlingId: UUID): Boolean {
        this["@event_name"].asText() shouldBe "saksbehandling_alert"
        this["alertType"].asText() shouldBe "OPPGAVE_IKKE_FUNNET"
        this["feilMelding"].asText() shouldBe "Oppgave ikke funnet"
        this["utvidetFeilMelding"].asText() shouldContain behandlingId.toString()
        return true
    }
}
