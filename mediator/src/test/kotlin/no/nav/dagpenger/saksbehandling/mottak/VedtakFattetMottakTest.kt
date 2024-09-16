package no.nav.dagpenger.saksbehandling.mottak

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.helper.vedtakFattetHendelse
import no.nav.dagpenger.saksbehandling.helper.vedtakFattetHendelseMedMeldingOmVedtakProdusent
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator
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
                    person =
                        Person(
                            id = UUIDv7.ny(),
                            ident = testIdent,
                            skjermesSomEgneAnsatte = false,
                            adressebeskyttelseGradering = UGRADERT,
                        ),
                    opprettet = opprettet,
                ),
        )

    private val testRapid = TestRapid()
    private val oppgaveMediatorMock = mockk<OppgaveMediator>(relaxed = true)
    private val utsendingMediatorMock = mockk<UtsendingMediator>()
//    private val utsendingRepositoryMock = mockk<UtsendingRepository>(relaxed = true)

    init {
        VedtakFattetMottak(testRapid, oppgaveMediatorMock, utsendingMediatorMock)
    }

    @Test
    fun `Skal behandle vedtak fattet hendelse når utsending ikke finnes`() {
        every { oppgaveMediatorMock.ferdigstillOppgave(any<VedtakFattetHendelse>()) } returns oppgave
        every { utsendingMediatorMock.utsendingFinnesForBehandling(behandlingId = oppgave.behandlingId) } returns false
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
        testRapid.inspektør.message(0).apply {
            this["@event_name"].asText() shouldBe "vedtak_fattet_til_arena"
            this["meldingOmVedtakProdusent"].asText() shouldBe "Arena"
        }
    }

    @Test
    fun `Skal behandle vedtak fattet hendelse når utsending finnes`() {
        every { oppgaveMediatorMock.ferdigstillOppgave(any<VedtakFattetHendelse>()) } returns oppgave
        every { utsendingMediatorMock.utsendingFinnesForBehandling(behandlingId = oppgave.behandlingId) } returns true
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
        testRapid.inspektør.message(0).apply {
            this["@event_name"].asText() shouldBe "vedtak_fattet_til_arena"
            this["meldingOmVedtakProdusent"].asText() shouldBe "Dagpenger"
        }
    }

    @Test
    fun `Skal ikke behandle vedtak fattet hendelser allerede beriket med meldingOmVedtakProdusent`() {
        every { oppgaveMediatorMock.ferdigstillOppgave(any<VedtakFattetHendelse>()) } returns oppgave
        testRapid.sendTestMessage(
            vedtakFattetHendelseMedMeldingOmVedtakProdusent(
                ident = testIdent,
                søknadId = søknadId,
                behandlingId = behandlingId,
                sakId = sak.id.toInt(),
            ),
        )
        verify(exactly = 0) {
            oppgaveMediatorMock.ferdigstillOppgave(any<VedtakFattetHendelse>())
        }
    }
}
