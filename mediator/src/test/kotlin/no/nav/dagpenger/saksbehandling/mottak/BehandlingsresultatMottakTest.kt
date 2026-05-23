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
            every { it.håndter(any(), any()) } just Runs
        }

    init {
        BehandlingsresultatMottak(testRapid, oppgaveMediatorMock)
    }

    @Test
    fun `skal håndtere behandlingsresultat event og ferdigstille oppgave for søknad og automatisk behandlet`() {
        testRapid.sendTestMessage(behandlingsresultatEvent(behandletHendelseType = "Søknad"))
        verify(exactly = 1) {
            oppgaveMediatorMock.håndter(
                vedtakFattetHendelse =
                    VedtakFattetHendelse(
                        behandlingId = behandlingId,
                        behandletHendelseId = søknadId.toString(),
                        behandletHendelseType = "Søknad",
                        ident = TestHelper.testPerson.ident,
                        automatiskBehandlet = true,
                        sak = null,
                    ),
                emneknagger = any(),
            )
        }
    }

    @Test
    fun `skal håndtere behandlingsresultat event og ferdigstille oppgave for søknad og ikke automatisk `() {
        testRapid.sendTestMessage(behandlingsresultatEvent(behandletHendelseType = "Søknad", automatisk = false))
        verify(exactly = 1) {
            oppgaveMediatorMock.håndter(
                vedtakFattetHendelse =
                    VedtakFattetHendelse(
                        behandlingId = behandlingId,
                        behandletHendelseId = søknadId.toString(),
                        behandletHendelseType = "Søknad",
                        ident = TestHelper.testPerson.ident,
                        automatiskBehandlet = false,
                        sak = null,
                    ),
                emneknagger = any(),
            )
        }
    }

    @Test
    fun `skal håndtere behandlingsresultat event og ferdigstille oppgave for meldekort`() {
        testRapid.sendTestMessage(behandlingsresultatEvent(behandletHendelseType = "Meldekort"))
        verify(exactly = 1) {
            oppgaveMediatorMock.håndter(
                vedtakFattetHendelse =
                    VedtakFattetHendelse(
                        behandlingId = behandlingId,
                        behandletHendelseId = søknadId.toString(),
                        behandletHendelseType = "Meldekort",
                        ident = TestHelper.testPerson.ident,
                        automatiskBehandlet = true,
                        sak = null,
                    ),
                emneknagger = emptySet(),
            )
        }
    }

    @Test
    fun `skal håndtere behandlingsresultat event og ferdigstille oppgave for manuell`() {
        testRapid.sendTestMessage(behandlingsresultatEvent(behandletHendelseType = "Manuell"))
        verify(exactly = 1) {
            oppgaveMediatorMock.håndter(
                vedtakFattetHendelse =
                    VedtakFattetHendelse(
                        behandlingId = behandlingId,
                        behandletHendelseId = søknadId.toString(),
                        behandletHendelseType = "Manuell",
                        ident = TestHelper.testPerson.ident,
                        automatiskBehandlet = true,
                        sak = null,
                    ),
                emneknagger = emptySet(),
            )
        }
    }

    @Test
    fun `skal håndtere behandlingsresultat som kun er behandlet av saksbehandler`() {
        testRapid.sendTestMessage(
            behandlingsresultatEvent(
                behandletHendelseType = "Søknad",
                automatisk = false,
                saksbehandlerIdent = "MrSaksbehandler",
            ),
        )
        verify(exactly = 1) {
            oppgaveMediatorMock.håndter(
                vedtakFattetHendelse =
                    VedtakFattetHendelse(
                        behandlingId = behandlingId,
                        behandletHendelseId = søknadId.toString(),
                        behandletHendelseType = "Søknad",
                        ident = TestHelper.testPerson.ident,
                        automatiskBehandlet = false,
                        saksbehandlerIdent = "MrSaksbehandler",
                        sak = null,
                    ),
                emneknagger = any(),
            )
        }
    }

    @Test
    fun `skal håndtere behandlingsresultat som både er behandlet av saksbehandler og beslutter`() {
        testRapid.sendTestMessage(
            behandlingsresultatEvent(
                behandletHendelseType = "Søknad",
                automatisk = false,
                saksbehandlerIdent = "MrSaksbehandler",
                beslutterIdent = "MissBeslutter",
            ),
        )
        verify(exactly = 1) {
            oppgaveMediatorMock.håndter(
                vedtakFattetHendelse =
                    VedtakFattetHendelse(
                        behandlingId = behandlingId,
                        behandletHendelseId = søknadId.toString(),
                        behandletHendelseType = "Søknad",
                        ident = TestHelper.testPerson.ident,
                        automatiskBehandlet = false,
                        saksbehandlerIdent = "MrSaksbehandler",
                        beslutterIdent = "MissBeslutter",
                        sak = null,
                    ),
                emneknagger = any(),
            )
        }
    }

    @Test
    fun `skal håndtere behandlingsresultat som ikke har saksbehandler og beslutter - behandletAv er tomt array`() {
        //language=JSON
        val behandlingsresultat =
            """
            {
              "@event_name": "behandlingsresultat",
              "ident": "${TestHelper.testPerson.ident}",
              "behandlingId": "$behandlingId",
              "behandletHendelse": {
                "id": "$søknadId",
                "type": "Søknad"
              },
              "automatisk": true,
              "rettighetsperioder": [
                {
                  "fraOgMed": "2025-09-09",
                  "harRett": true
                }
              ],
              "behandletAv": []
            }
            """.trimIndent()
        testRapid.sendTestMessage(behandlingsresultat)
        verify(exactly = 1) {
            oppgaveMediatorMock.håndter(
                vedtakFattetHendelse =
                    VedtakFattetHendelse(
                        behandlingId = behandlingId,
                        behandletHendelseId = søknadId.toString(),
                        behandletHendelseType = "Søknad",
                        ident = TestHelper.testPerson.ident,
                        automatiskBehandlet = true,
                        saksbehandlerIdent = null,
                        beslutterIdent = null,
                        sak = null,
                    ),
                emneknagger = any(),
            )
        }
    }

    @Test
    fun `skal håndtere behandlingsresultat som ikke har saksbehandler og beslutter - behandletAv er missing`() {
        //language=JSON
        val behandlingsresultat =
            """
            {
              "@event_name": "behandlingsresultat",
              "ident": "${TestHelper.testPerson.ident}",
              "behandlingId": "$behandlingId",
              "behandletHendelse": {
                "id": "$søknadId",
                "type": "Søknad"
              },
              "automatisk": true,
              "rettighetsperioder": [
                {
                  "fraOgMed": "2025-09-09",
                  "harRett": true
                }
              ]
            }
            """.trimIndent()
        testRapid.sendTestMessage(behandlingsresultat)
        verify(exactly = 1) {
            oppgaveMediatorMock.håndter(
                vedtakFattetHendelse =
                    VedtakFattetHendelse(
                        behandlingId = behandlingId,
                        behandletHendelseId = søknadId.toString(),
                        behandletHendelseType = "Søknad",
                        ident = TestHelper.testPerson.ident,
                        automatiskBehandlet = true,
                        saksbehandlerIdent = null,
                        beslutterIdent = null,
                        sak = null,
                    ),
                emneknagger = any(),
            )
        }
    }

    private fun behandlingsresultatEvent(
        ident: String = TestHelper.testPerson.ident,
        behandlingId: String = this.behandlingId.toString(),
        søknadId: String = this.søknadId.toString(),
        automatisk: Boolean = true,
        behandletHendelseType: String = "Søknad",
        saksbehandlerIdent: String? = null,
        beslutterIdent: String? = null,
    ): String {
        val saksbehandlerJson =
            if (saksbehandlerIdent != null) {
                """{ "rolle": "saksbehandler", "behandler": { "ident": "$saksbehandlerIdent" }}""".trimIndent() +
                    if (beslutterIdent != null) {
                        ",".trimIndent()
                    } else {
                        ""
                    }
            } else {
                ""
            }
        val beslutterJson =
            if (beslutterIdent != null) {
                """{ "rolle": "beslutter", "behandler": { "ident": "$beslutterIdent" }}""".trimIndent()
            } else {
                ""
            }
        val behandletAvJson: String =
            if (saksbehandlerIdent != null || beslutterIdent != null) {
                """
                ,
                "behandletAv": [
                $saksbehandlerJson $beslutterJson
                ]
                """.trimIndent()
            } else {
                ""
            }
        //language=JSON
        val json =
            """
            {
              "@event_name": "behandlingsresultat",
              "ident": "$ident",
              "behandlingId": "$behandlingId",
              "behandletHendelse": {
                "id": "$søknadId",
                "type": "$behandletHendelseType"
              },
              "automatisk": $automatisk,
              "saksbehandlerIdent": null,
              "beslutterIdent": null,
              "opplysninger": [],
              "rettighetsperioder": [] $behandletAvJson
            }
            """.trimIndent()
        return json
    }
}
