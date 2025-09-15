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
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class BehandlingsResultatMottakTest {
    private val testIdent = "12345678901"
    private val søknadId = UUID.randomUUID()
    private val behandlingId = UUID.randomUUID()
    private val behandlingIdUtenOppgave = UUID.randomUUID()
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
    private val oppgaveMediatorMock =
        mockk<OppgaveMediator>().also {
            every { it.hentOppgaveIdFor(behandlingId) } returns oppgave.oppgaveId
            every { it.hentOppgaveIdFor(behandlingIdUtenOppgave) } returns null
            every { it.ferdigstillOppgave(any<VedtakFattetHendelse>()) } returns oppgave
        }

    init {
        BehandlingsResultatMottak(testRapid, oppgaveMediatorMock)
    }

    @Test
    fun `skal håndtere behandlingsresultat event og ferdigstille oppgave for søknad og automatisk behandlet`() {
        testRapid.sendTestMessage(behandlingResultatEvent(behandletHendelseType = "Søknad"))
        verify(exactly = 1) {
            oppgaveMediatorMock.ferdigstillOppgave(
                VedtakFattetHendelse(
                    behandlingId = behandlingId,
                    behandletHendelseId = søknadId.toString(),
                    behandletHendelseType = "Søknad",
                    ident = testIdent,
                    automatiskBehandlet = true,
                    sak = utsendingSak,
                ),
            )
        }
    }

    @Test
    fun `skal håndtere behandlingsresultat event og ferdigstille oppgave for søknad og ikke automatisk `() {
        testRapid.sendTestMessage(behandlingResultatEvent(behandletHendelseType = "Søknad", automatisk = false))
        verify(exactly = 1) {
            oppgaveMediatorMock.ferdigstillOppgave(
                VedtakFattetHendelse(
                    behandlingId = behandlingId,
                    behandletHendelseId = søknadId.toString(),
                    behandletHendelseType = "Søknad",
                    ident = testIdent,
                    automatiskBehandlet = false,
                    sak = utsendingSak,
                ),
            )
        }
    }

    @Test
    fun `skal håndtere behandlingsresultat event og ferdigstille oppgave for meldekort`() {
        testRapid.sendTestMessage(behandlingResultatEvent(behandletHendelseType = "Meldekort"))
        verify(exactly = 1) {
            oppgaveMediatorMock.ferdigstillOppgave(
                VedtakFattetHendelse(
                    behandlingId = behandlingId,
                    behandletHendelseId = søknadId.toString(),
                    behandletHendelseType = "Meldekort",
                    ident = testIdent,
                    automatiskBehandlet = true,
                    sak = utsendingSak,
                ),
            )
        }
    }

    @Test
    fun `skal håndtere behandlingsresultat event og ferdigstille oppgave for manuell`() {
        testRapid.sendTestMessage(behandlingResultatEvent(behandletHendelseType = "Manuell"))
        verify(exactly = 1) {
            oppgaveMediatorMock.ferdigstillOppgave(
                VedtakFattetHendelse(
                    behandlingId = behandlingId,
                    behandletHendelseId = søknadId.toString(),
                    behandletHendelseType = "Manuell",
                    ident = testIdent,
                    automatiskBehandlet = true,
                    sak = utsendingSak,
                ),
            )
        }
    }

    @Test
    fun `Skal ignorere hendelsen hvis det ikke finnes noen oppgave for behandlingen`() {
        testRapid.sendTestMessage(behandlingResultatEvent(behandlingId = behandlingIdUtenOppgave.toString()))

        verify(exactly = 1) { oppgaveMediatorMock.hentOppgaveIdFor(behandlingIdUtenOppgave) }
        verify(exactly = 0) { oppgaveMediatorMock.ferdigstillOppgave(any<VedtakFattetHendelse>()) }
    }

    private fun behandlingResultatEvent(
        ident: String = this.testIdent,
        behandlingId: String = this.behandlingId.toString(),
        søknadId: String = this.søknadId.toString(),
        automatisk: Boolean = true,
        behandletHendelseType: String = "Søknad",
    ): String {
        //language=JSON
        return """
            {
              "@event_name": "behandlingsresultat",
              "ident": "$ident",
              "behandlingId": "$behandlingId",
              "behandletHendelse": {
                "id": "$søknadId",
                "type": "$behandletHendelseType"
              },
              "automatisk": $automatisk,
              "opplysninger": [
                {
                  "opplysningTypeId": "0194881f-9462-78af-8977-46092bb030eb",
                  "navn": "fagsakId",
                  "perioder": [
                    {
                      "id": "01992e40-e7ad-7d83-8733-e4fc7a0f1cd8",
                      "opprettet": "2025-09-09T13:33:49.61359",
                      "status": "Ny",
                      "verdi": {
                        "verdi": ${utsendingSak.id},
                        "datatype": "heltall"
                      },
                      "kilde": {
                        "type": "System",
                        "registrert": "2025-09-09T13:33:49.613585",
                        "meldingId": "eff2e3d2-d956-419c-96e1-0e3da0105ded"
                      }
                    }
                  ]
                }
              ]
            }
            """.trimIndent()
    }
}
