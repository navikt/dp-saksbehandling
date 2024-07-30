package no.nav.dagpenger.saksbehandling.mottak

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.pdl.PDLKlient
import no.nav.dagpenger.saksbehandling.skjerming.SkjermingKlient
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class BehandligOpprettetMottakTest {
    val testIdent = "12345678901"
    val søknadId = UUID.randomUUID()
    val behandlingId = UUID.randomUUID()
    val opprettet = LocalDateTime.parse("2024-02-27T10:41:52.800935377")
    private val søknadsbehandlingOpprettetHendelse =
        SøknadsbehandlingOpprettetHendelse(
            søknadId = søknadId,
            behandlingId = behandlingId,
            ident = testIdent,
            opprettet = opprettet,
        )

    private val testRapid = TestRapid()
    private val oppgaveMediatorMock = mockk<OppgaveMediator>(relaxed = true)
    val pdlKlientMock =
        mockk<PDLKlient>().also {
            coEvery { it.erAdressebeskyttet(testIdent) }.returns(Result.success(false))
        }

    val skjermetKlientMock =
        mockk<SkjermingKlient>().also {
            coEvery { it.erSkjermetPerson(testIdent) }.returns(Result.success(false))
        }

    init {
        BehandlingOpprettetMottak(testRapid, oppgaveMediatorMock, pdlKlientMock, skjermetKlientMock)
    }

    @Test
    fun `Skal behandle behandling_opprettet hendelse`() {
        testRapid.sendTestMessage(behandlingOpprettetMelding())
        verify(exactly = 1) {
            oppgaveMediatorMock.opprettOppgaveForBehandling(søknadsbehandlingOpprettetHendelse)
        }
    }

    @Test
    fun `Avbryt behandling og ikke lag oppgave som gjelder adressebeskyttede personer`() {
        val adressebeskyttetIdent = "11111222222"

        coEvery { pdlKlientMock.erAdressebeskyttet(adressebeskyttetIdent) }.returns(Result.success(true))
        coEvery { skjermetKlientMock.erSkjermetPerson(adressebeskyttetIdent) }.returns(Result.success(false))

        testRapid.sendTestMessage(behandlingOpprettetMelding(adressebeskyttetIdent))

        verify(exactly = 0) {
            oppgaveMediatorMock.opprettOppgaveForBehandling(any<SøknadsbehandlingOpprettetHendelse>())
        }

        testRapid.inspektør.size shouldBe 1
        val message = testRapid.inspektør.message(0)
        message["@event_name"].asText() shouldBe "avbryt_behandling"
        message["behandlingId"].asUUID() shouldBe behandlingId
        message["søknadId"].asUUID() shouldBe søknadId
        message["ident"].asText() shouldBe adressebeskyttetIdent
    }

    @Test
    fun `Avbryt behandling og ikke lag oppgave som gjelder personer som skjermes som egne ansatte`() {
        val adressebeskyttetIdent = "11111222222"
        coEvery { pdlKlientMock.erAdressebeskyttet(adressebeskyttetIdent) }.returns(Result.success(false))
        coEvery { skjermetKlientMock.erSkjermetPerson(adressebeskyttetIdent) }.returns(Result.success(true))

        testRapid.sendTestMessage(behandlingOpprettetMelding(adressebeskyttetIdent))

        verify(exactly = 0) {
            oppgaveMediatorMock.opprettOppgaveForBehandling(any<SøknadsbehandlingOpprettetHendelse>())
        }

        testRapid.inspektør.size shouldBe 1
        val message = testRapid.inspektør.message(0)
        message["@event_name"].asText() shouldBe "avbryt_behandling"
        message["behandlingId"].asUUID() shouldBe behandlingId
        message["søknadId"].asUUID() shouldBe søknadId
        message["ident"].asText() shouldBe adressebeskyttetIdent
    }

    @Test
    fun `Avbryt behandling og ikke lag oppgave som gjelder personer som skjermes som egne ansatte og er adressebeskyttet`() {
        val adressebeskyttetIdent = "11111222222"
        coEvery { pdlKlientMock.erAdressebeskyttet(adressebeskyttetIdent) }.returns(Result.success(true))
        coEvery { skjermetKlientMock.erSkjermetPerson(adressebeskyttetIdent) }.returns(Result.success(true))

        testRapid.sendTestMessage(behandlingOpprettetMelding(adressebeskyttetIdent))

        verify(exactly = 0) {
            oppgaveMediatorMock.opprettOppgaveForBehandling(any<SøknadsbehandlingOpprettetHendelse>())
        }

        testRapid.inspektør.size shouldBe 1
        val message = testRapid.inspektør.message(0)
        message["@event_name"].asText() shouldBe "avbryt_behandling"
        message["behandlingId"].asUUID() shouldBe behandlingId
        message["søknadId"].asUUID() shouldBe søknadId
        message["ident"].asText() shouldBe adressebeskyttetIdent
    }

    @Language("JSON")
    private fun behandlingOpprettetMelding(ident: String = testIdent) =
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
