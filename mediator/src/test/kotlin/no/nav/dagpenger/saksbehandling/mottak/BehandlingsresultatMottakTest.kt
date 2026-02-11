package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.TestHelper
import no.nav.dagpenger.saksbehandling.TestHelper.lagBehandling
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class BehandlingsresultatMottakTest {
    private val søknadId = UUID.randomUUID()
    private val behandlingId = UUID.randomUUID()
    private val behandlingIdUtenOppgave = UUID.randomUUID()
    private val opprettet = LocalDateTime.parse("2024-02-27T10:41:52.800935377")
    private val oppgave =
        TestHelper.lagOppgave(
            opprettet = opprettet,
            person = TestHelper.testPerson,
            behandling = lagBehandling(behandlingId = behandlingId),
        )

    private val testRapid = TestRapid()
    private val oppgaveMediatorMock =
        mockk<OppgaveMediator>().also {
            every { it.håndter(any()) } just Runs
        }

    init {
        BehandlingsresultatMottak(testRapid, oppgaveMediatorMock)
    }

    @Test
    fun `skal håndtere behandlingsresultat event og ferdigstille oppgave for søknad og automatisk behandlet`() {
        testRapid.sendTestMessage(behandlingsresultatEvent(behandletHendelseType = "Søknad"))
        verify(exactly = 1) {
            oppgaveMediatorMock.håndter(
                VedtakFattetHendelse(
                    behandlingId = behandlingId,
                    behandletHendelseId = søknadId.toString(),
                    behandletHendelseType = "Søknad",
                    ident = TestHelper.testPerson.ident,
                    automatiskBehandlet = true,
                    sak = null,
                ),
            )
        }
    }

    @Test
    fun `skal håndtere behandlingsresultat event og ferdigstille oppgave for søknad og ikke automatisk `() {
        testRapid.sendTestMessage(behandlingsresultatEvent(behandletHendelseType = "Søknad", automatisk = false))
        verify(exactly = 1) {
            oppgaveMediatorMock.håndter(
                VedtakFattetHendelse(
                    behandlingId = behandlingId,
                    behandletHendelseId = søknadId.toString(),
                    behandletHendelseType = "Søknad",
                    ident = TestHelper.testPerson.ident,
                    automatiskBehandlet = false,
                    sak = null,
                ),
            )
        }
    }

    @Test
    fun `skal håndtere behandlingsresultat event og ferdigstille oppgave for meldekort`() {
        testRapid.sendTestMessage(behandlingsresultatEvent(behandletHendelseType = "Meldekort"))
        verify(exactly = 1) {
            oppgaveMediatorMock.håndter(
                VedtakFattetHendelse(
                    behandlingId = behandlingId,
                    behandletHendelseId = søknadId.toString(),
                    behandletHendelseType = "Meldekort",
                    ident = TestHelper.testPerson.ident,
                    automatiskBehandlet = true,
                    sak = null,
                ),
            )
        }
    }

    @Test
    fun `skal håndtere behandlingsresultat event og ferdigstille oppgave for manuell`() {
        testRapid.sendTestMessage(behandlingsresultatEvent(behandletHendelseType = "Manuell"))
        verify(exactly = 1) {
            oppgaveMediatorMock.håndter(
                VedtakFattetHendelse(
                    behandlingId = behandlingId,
                    behandletHendelseId = søknadId.toString(),
                    behandletHendelseType = "Manuell",
                    ident = TestHelper.testPerson.ident,
                    automatiskBehandlet = true,
                    sak = null,
                ),
            )
        }
    }

    private fun behandlingsresultatEvent(
        ident: String = TestHelper.testPerson.ident,
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
              "rettighetsperioder": [ ]
                }
              ]
            }
            """.trimIndent()
    }
}
