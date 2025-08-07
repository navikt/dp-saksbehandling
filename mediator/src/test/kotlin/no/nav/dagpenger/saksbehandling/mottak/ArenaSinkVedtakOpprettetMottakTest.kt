package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.UtsendingSak
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import no.nav.dagpenger.saksbehandling.lagOppgave
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator
import org.junit.jupiter.api.Test

class ArenaSinkVedtakOpprettetMottakTest {
    private val testRapid = TestRapid()
    private val sakId = "123"
    private val søknadId = UUIDv7.ny()
    private val testOppgave = lagOppgave(tilstand = Oppgave.FerdigBehandlet)
    private val oppgaveRepository =
        mockk<OppgaveRepository>(relaxed = true).apply {
            every { hentOppgaveFor(testOppgave.behandlingId) } returns testOppgave
        }

    @Test
    fun `Skal ta imot arenasink_vedtak_opprettet hendelser`() {
        val forventetVedtakFattetNendelse =
            VedtakFattetHendelse(
                behandlingId = testOppgave.behandlingId,
                behandletHendelseId = søknadId.toString(),
                behandletHendelseType = "Søknad",
                ident = testOppgave.person.ident,
                sak =
                    UtsendingSak(
                        id = sakId,
                        kontekst = "Arena",
                    ),
                automatiskBehandlet = null,
            )
        val mockUtsendingMediator =
            mockk<UtsendingMediator>().also {
                every { it.utsendingFinnesForOppgave(oppgaveId = testOppgave.oppgaveId) } returns true
                every { it.startUtsendingForVedtakFattet(forventetVedtakFattetNendelse) } just Runs
            }
        val mockSakMediator =
            mockk<SakMediator>().also {
                every { it.oppdaterSakMedArenaSakId(forventetVedtakFattetNendelse) } just Runs
            }

        ArenaSinkVedtakOpprettetMottak(
            rapidsConnection = testRapid,
            oppgaveRepository = oppgaveRepository,
            utsendingMediator = mockUtsendingMediator,
            sakMediator = mockSakMediator,
        )

        testRapid.sendTestMessage(arenaSinkVedtakOpprettetHendelse)
        verify(exactly = 1) {
            oppgaveRepository.hentOppgaveFor(testOppgave.behandlingId)
            mockUtsendingMediator.startUtsendingForVedtakFattet(forventetVedtakFattetNendelse)
        }
    }

    @Test
    fun `Skal ikke gjøre noe dersom vedtak ikke iverksetttes i Arena`() {
        ArenaSinkVedtakOpprettetMottak(
            rapidsConnection = testRapid,
            oppgaveRepository = oppgaveRepository,
            utsendingMediator = mockk(),
            sakMediator = mockk(),
        )

        val vedtakOpprettetMenIkkeIverksattMelding =
            arenaSinkVedtakOpprettetHendelse.replace(VEDTAKSTATUS_IVERKSATT, "Muse Mikk")
        testRapid.sendTestMessage(vedtakOpprettetMenIkkeIverksattMelding)
        verify(exactly = 1) {
            oppgaveRepository.hentOppgaveFor(testOppgave.behandlingId)
        }

        testRapid.inspektør.size shouldBe 0
    }

    private val arenaSinkVedtakOpprettetHendelse =
        //language=JSON
        """
        {
          "@event_name": "arenasink_vedtak_opprettet",
          "søknadId": "$søknadId",
          "sakId": $sakId,
          "vedtakId": 0,
          "vedtakstatus": "IVERK",
          "rettighet": "PERM",
          "utfall": false,
          "kilde": {
            "id": "${testOppgave.behandlingId}",
            "system": "dp-behandling"
          },
          "@id": "b525ed15-e041-4d80-a20c-2a26885eae75",
          "@opprettet": "2024-09-06T09:19:14.861556",
          "system_read_count": 0,
          "system_participating_services": [
            {
              "id": "b525ed15-e041-4d80-a20c-2a26885eae75",
              "time": "2024-09-06T09:19:14.861556"
            }
          ]
        }
        """.trimIndent()
}
