package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.BehandlingType
import no.nav.dagpenger.saksbehandling.Oppgave
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
            behandlingType = BehandlingType.RETT_TIL_DAGPENGER,
            person = person,
        )

    private val testRapid = TestRapid()
    private val oppgaveMediatorMock = mockk<OppgaveMediator>(relaxed = true)

    init {
        VedtakFattetMottak(testRapid, oppgaveMediatorMock)
    }

    @Test
    fun `Skal behandle vedtak fattet hendelse når utsending ikke finnes`() {
        every { oppgaveMediatorMock.ferdigstillOppgave(any<VedtakFattetHendelse>()) } returns oppgave
        testRapid.sendTestMessage(
            vedtakFattetHendelse(
                ident = testIdent,
                søknadId = søknadId,
                behandlingId = behandlingId,
                automatiskBehandlet = true,
                sakId = utsendingSak.id.toInt(),
            ),
        )

        val vedtakFattetHendelse =
            VedtakFattetHendelse(
                behandlingId = behandlingId,
                id = søknadId.toString(),
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
        every { oppgaveMediatorMock.ferdigstillOppgave(any<VedtakFattetHendelse>()) } returns oppgave
        testRapid.sendTestMessage(
            vedtakFattetHendelse(
                ident = testIdent,
                søknadId = søknadId,
                behandlingId = behandlingId,
                automatiskBehandlet = false,
                sakId = utsendingSak.id.toInt(),
            ),
        )

        val vedtakFattetHendelse =
            VedtakFattetHendelse(
                behandlingId = behandlingId,
                id = søknadId.toString(),
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
    fun `Leser ny versjon av vedtak_fattet innhold`() {
        every { oppgaveMediatorMock.ferdigstillOppgave(any<VedtakFattetHendelse>()) } returns oppgave
        val vedtakFattetFil = "/vedtak_fattet.json"
        val vedtakFattet = this.javaClass.getResource(vedtakFattetFil)?.readText() ?: throw AssertionError("Fant ikke fil $vedtakFattetFil")
        testRapid.sendTestMessage(vedtakFattet)

        verify(exactly = 1) {
            oppgaveMediatorMock.ferdigstillOppgave(any<VedtakFattetHendelse>())
        }
    }
}
