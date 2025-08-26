package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.BehandlingType.RETT_TIL_DAGPENGER
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Oppgave.MeldingOmVedtakKilde.DP_SAK
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.UtsendingSak
import no.nav.dagpenger.saksbehandling.helper.vedtakFattetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class VedtakFattetMottakTest {
    private val testIdent = "12345678901"
    private val søknadId = UUID.randomUUID()
    private val behandlingId = UUID.randomUUID()
    private val utsendingSak = UtsendingSak("12342", "Arena")
    private val opprettet = LocalDateTime.parse("2024-02-27T10:41:52.800935377")
    private val person =
        Person(
            id = UUIDv7.ny(),
            ident = testIdent,
            skjermesSomEgneAnsatte = false,
            adressebeskyttelseGradering = UGRADERT,
        )
    private val oppgave =
        Oppgave(
            oppgaveId = UUIDv7.ny(),
            opprettet = opprettet,
            behandlingId = behandlingId,
            behandlingType = RETT_TIL_DAGPENGER,
            person = person,
            meldingOmVedtak =
                Oppgave.MeldingOmVedtak(
                    kilde = DP_SAK,
                    kontrollertGosysBrev = Oppgave.KontrollertBrev.IKKE_RELEVANT,
                ),
        )

    private val testRapid = TestRapid()
    private val oppgaveMediatorMock = mockk<OppgaveMediator>()

    init {
        VedtakFattetMottak(testRapid, oppgaveMediatorMock)
    }

    @Test
    fun `Skal behandle vedtak fattet hendelse når utsending ikke finnes`() {
        every { oppgaveMediatorMock.hentOppgaveIdFor(behandlingId) } returns oppgave.oppgaveId
        every { oppgaveMediatorMock.ferdigstillOppgave(any<VedtakFattetHendelse>()) } returns oppgave
        testRapid.sendTestMessage(
            vedtakFattetHendelse(
                ident = testIdent,
                behandletHendelseId = søknadId.toString(),
                behandlingId = behandlingId,
                automatiskBehandlet = true,
                sakId = utsendingSak.id.toInt(),
            ),
        )

        val vedtakFattetHendelse =
            VedtakFattetHendelse(
                behandlingId = behandlingId,
                behandletHendelseId = søknadId.toString(),
                behandletHendelseType = "Søknad",
                ident = testIdent,
                automatiskBehandlet = true,
                sak = utsendingSak,
            )
        verify(exactly = 1) {
            oppgaveMediatorMock.ferdigstillOppgave(vedtakFattetHendelse)
        }
    }

    @Test
    fun `Skal behandle vedtak fattet hendelse når utsending finnes`() {
        every { oppgaveMediatorMock.hentOppgaveIdFor(behandlingId) } returns oppgave.oppgaveId
        every { oppgaveMediatorMock.ferdigstillOppgave(any<VedtakFattetHendelse>()) } returns oppgave
        testRapid.sendTestMessage(
            vedtakFattetHendelse(
                ident = testIdent,
                behandletHendelseId = søknadId.toString(),
                behandlingId = behandlingId,
                automatiskBehandlet = false,
                sakId = utsendingSak.id.toInt(),
            ),
        )

        val vedtakFattetHendelse =
            VedtakFattetHendelse(
                behandlingId = behandlingId,
                behandletHendelseId = søknadId.toString(),
                behandletHendelseType = "Søknad",
                ident = testIdent,
                automatiskBehandlet = false,
                sak = utsendingSak,
            )
        verify(exactly = 1) {
            oppgaveMediatorMock.ferdigstillOppgave(vedtakFattetHendelse)
        }
    }

    @Test
    fun `Skal behandle vedtak fattet hendelse for meldekorthendelse`() {
        val meldekortId = "12345"
        every { oppgaveMediatorMock.ferdigstillOppgave(any<VedtakFattetHendelse>()) } returns oppgave
        every { oppgaveMediatorMock.hentOppgaveIdFor(behandlingId) } returns oppgave.oppgaveId
        testRapid.sendTestMessage(
            vedtakFattetHendelse(
                ident = testIdent,
                behandletHendelseId = meldekortId,
                behandletHendelseType = "Meldekort",
                behandlingId = behandlingId,
                automatiskBehandlet = false,
                sakId = utsendingSak.id.toInt(),
            ),
        )

        val vedtakFattetHendelse =
            VedtakFattetHendelse(
                behandlingId = behandlingId,
                behandletHendelseId = meldekortId,
                behandletHendelseType = "Meldekort",
                ident = testIdent,
                automatiskBehandlet = false,
                sak = utsendingSak,
            )
        verify(exactly = 1) {
            oppgaveMediatorMock.ferdigstillOppgave(vedtakFattetHendelse)
        }
    }

    @Test
    fun `Skal behandle vedtak fattet hendelse for manuell`() {
        val manuellId = UUIDv7.ny()
        every { oppgaveMediatorMock.ferdigstillOppgave(any<VedtakFattetHendelse>()) } returns oppgave
        every { oppgaveMediatorMock.hentOppgaveIdFor(behandlingId) } returns oppgave.oppgaveId
        testRapid.sendTestMessage(
            vedtakFattetHendelse(
                ident = testIdent,
                behandletHendelseId = manuellId.toString(),
                behandletHendelseType = "Manuell",
                behandlingId = behandlingId,
                automatiskBehandlet = false,
                sakId = utsendingSak.id.toInt(),
            ),
        )

        val vedtakFattetHendelse =
            VedtakFattetHendelse(
                behandlingId = behandlingId,
                behandletHendelseId = manuellId.toString(),
                behandletHendelseType = "Manuell",
                ident = testIdent,
                automatiskBehandlet = false,
                sak = utsendingSak,
            )
        verify(exactly = 1) {
            oppgaveMediatorMock.ferdigstillOppgave(vedtakFattetHendelse)
        }
    }

    @Test
    fun `Skal ignorere hendelsen hvis det ikke finnes noen oppgave for behandlingen`() {
        every { oppgaveMediatorMock.hentOppgaveIdFor(behandlingId) } returns null
        testRapid.sendTestMessage(
            vedtakFattetHendelse(
                ident = testIdent,
                behandletHendelseId = søknadId.toString(),
                behandlingId = behandlingId,
                automatiskBehandlet = true,
                sakId = utsendingSak.id.toInt(),
            ),
        )

        val vedtakFattetHendelse =
            VedtakFattetHendelse(
                behandlingId = behandlingId,
                behandletHendelseId = søknadId.toString(),
                behandletHendelseType = "Søknad",
                ident = testIdent,
                automatiskBehandlet = true,
                sak = utsendingSak,
            )
        verify(exactly = 0) {
            oppgaveMediatorMock.ferdigstillOppgave(vedtakFattetHendelse)
        }
    }

    @Test
    fun `Leser ny versjon av vedtak_fattet innhold`() {
        every { oppgaveMediatorMock.hentOppgaveIdFor(UUID.fromString("0195b37b-aaef-7cca-b397-3ef004deaf9f")) } returns oppgave.oppgaveId
        every { oppgaveMediatorMock.ferdigstillOppgave(any<VedtakFattetHendelse>()) } returns oppgave
        val vedtakFattetFil = "/vedtak_fattet.json"
        val vedtakFattet = this.javaClass.getResource(vedtakFattetFil)?.readText() ?: throw AssertionError("Fant ikke fil $vedtakFattetFil")
        testRapid.sendTestMessage(vedtakFattet)

        verify(exactly = 1) {
            oppgaveMediatorMock.ferdigstillOppgave(any<VedtakFattetHendelse>())
        }
    }
}
