package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.db.lagOppgave
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator
import org.junit.jupiter.api.Test

class ArenaSinkVedtakOpprettetMottakTest {
    private val testRapid = TestRapid()
    private val sakId = "123"
    private val testOppgave = lagOppgave(tilstand = Oppgave.FerdigBehandlet)
    private val oppgaveRepository =
        mockk<OppgaveRepository>(relaxed = true).apply {
            every { hentOppgaveFor(testOppgave.behandlingId) } returns testOppgave
        }

    @Test
    fun `Skal ta imot arenasink_vedtak_opprettet hendelser`() {
        val utsendingRepository =
            mockk<UtsendingMediator>().also {
                every { it.utsendingFinnesForOppgave(oppgaveId = testOppgave.oppgaveId) } returns true
            }

        ArenaSinkVedtakOpprettetMottak(
            testRapid,
            oppgaveRepository,
            utsendingRepository,
        )

        testRapid.sendTestMessage(arenaSinkVedtakOpprettetHendelse)
        verify(exactly = 1) {
            oppgaveRepository.hentOppgaveFor(testOppgave.behandlingId)
        }

        val startUtsendingEvent = testRapid.inspektør.message(0)

        startUtsendingEvent["@event_name"].asText() shouldBe "start_utsending"
        startUtsendingEvent["behandlingId"].asUUID() shouldBe testOppgave.behandlingId
        startUtsendingEvent["oppgaveId"].asUUID() shouldBe testOppgave.oppgaveId
        startUtsendingEvent["ident"].asText() shouldBe testOppgave.ident
        startUtsendingEvent["sak"]["id"].asText() shouldBe sakId
        startUtsendingEvent["sak"]["kontekst"].asText() shouldBe "Arena"
    }

    @Test
    fun `Ikke send start_utsending event når vedtakstatus != IVERK`() {
        ArenaSinkVedtakOpprettetMottak(
            testRapid,
            oppgaveRepository,
            mockk(),
        )

        val vedtakOpprettetMenIkkeIverksattMelding =
            arenaSinkVedtakOpprettetHendelse.replace(VEDTAKSTATUS_IVERKSATT, "Muse Mikk")
        testRapid.sendTestMessage(vedtakOpprettetMenIkkeIverksattMelding)
        verify(exactly = 1) {
            oppgaveRepository.hentOppgaveFor(testOppgave.behandlingId)
        }

        testRapid.inspektør.size shouldBe 0
    }

    @Test
    fun `Skal ikke sende ut start utsending events når utsending ikke finnes `() {
        ArenaSinkVedtakOpprettetMottak(
            testRapid,
            oppgaveRepository,
            mockk<UtsendingMediator>().also { coEvery { it.utsendingFinnesForOppgave(testOppgave.oppgaveId) } returns false },
        )
        testRapid.sendTestMessage(arenaSinkVedtakOpprettetHendelse)
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
          "søknadId": "4afce924-6cb4-4ab4-a92b-fe91e24f31bf",
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
