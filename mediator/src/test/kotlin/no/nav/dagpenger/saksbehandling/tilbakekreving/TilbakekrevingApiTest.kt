package no.nav.dagpenger.saksbehandling.tilbakekreving

import io.kotest.assertions.json.shouldEqualSpecifiedJson
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.HendelseBehandler
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.OppgaveTilstandslogg
import no.nav.dagpenger.saksbehandling.TestHelper.lagBehandling
import no.nav.dagpenger.saksbehandling.TestHelper.lagOppgave
import no.nav.dagpenger.saksbehandling.Tilstandsendring
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.api.MockAzure.Companion.autentisert
import no.nav.dagpenger.saksbehandling.api.installerApis
import no.nav.dagpenger.saksbehandling.api.mockAzure
import no.nav.dagpenger.saksbehandling.audit.TestAuditlogg
import no.nav.dagpenger.saksbehandling.db.oppgave.DataNotFoundException
import no.nav.dagpenger.saksbehandling.hendelser.Tilbakekreving
import no.nav.dagpenger.saksbehandling.hendelser.Tilbakekreving.BehandlingStatus
import no.nav.dagpenger.saksbehandling.hendelser.Tilbakekreving.BehandlingStatus.TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.hendelser.TilbakekrevingHendelse
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class TilbakekrevingApiTest {
    init {
        mockAzure()
    }

    private val tilbakekrevingBehandlingId = UUIDv7.ny()

    @Test
    fun `Skal kaste feil når det mangler autentisering`() {
        withTilbakekrevingApi(mockk(relaxed = true)) {
            client.get("tilbakekreving/$tilbakekrevingBehandlingId").status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `Skal returnere 404 når oppgaven ikke finnes`() {
        val oppgaveMediator =
            mockk<OppgaveMediator>().also {
                every { it.hentOppgaveForBehandling(any(), any()) } throws
                    DataNotFoundException("Fant ikke oppgave for behandlingId $tilbakekrevingBehandlingId")
            }
        withTilbakekrevingApi(oppgaveMediator) {
            client
                .get("tilbakekreving/$tilbakekrevingBehandlingId") {
                    autentisert()
                }.status shouldBe HttpStatusCode.NotFound
        }
    }

    @Test
    fun `Skal returnere 404 når oppgaven ikke er utløst av tilbakekreving`() {
        val oppgave =
            lagOppgave(
                behandling =
                    lagBehandling(
                        behandlingId = tilbakekrevingBehandlingId,
                        utløstAvType = HendelseBehandler.DpBehandling.Søknad,
                    ),
            )
        withTilbakekrevingApi(oppgaveMediatorSomReturnerer(oppgave)) {
            client
                .get("tilbakekreving/$tilbakekrevingBehandlingId") {
                    autentisert()
                }.status shouldBe HttpStatusCode.NotFound
        }
    }

    @Test
    fun `Skal returnere 404 når tilstandsloggen ikke inneholder en TilbakekrevingHendelse`() {
        val oppgave =
            lagOppgave(
                behandling =
                    lagBehandling(
                        behandlingId = tilbakekrevingBehandlingId,
                        utløstAvType = HendelseBehandler.Intern.Tilbakekreving,
                    ),
            )
        withTilbakekrevingApi(oppgaveMediatorSomReturnerer(oppgave)) {
            client
                .get("tilbakekreving/$tilbakekrevingBehandlingId") {
                    autentisert()
                }.status shouldBe HttpStatusCode.NotFound
        }
    }

    @Test
    fun `Skal returnere tilbakekreving for gyldig behandlingId`() {
        val hendelse = lagTilbakekrevingHendelse()
        val oppgave =
            lagOppgave(
                tilstandslogg =
                    OppgaveTilstandslogg(
                        Tilstandsendring(
                            tilstand = Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING,
                            hendelse = hendelse,
                        ),
                    ),
                behandling =
                    lagBehandling(
                        behandlingId = tilbakekrevingBehandlingId,
                        opprettet = hendelse.hendelseOpprettet,
                        utløstAvType = HendelseBehandler.Intern.Tilbakekreving,
                        hendelse = hendelse,
                    ),
            )
        withTilbakekrevingApi(oppgaveMediatorSomReturnerer(oppgave)) {
            val response =
                client.get("tilbakekreving/$tilbakekrevingBehandlingId") {
                    autentisert()
                }
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldEqualSpecifiedJson
                //language=json
                """
                {
                  "tilbakekrevingBehandlingId": "$tilbakekrevingBehandlingId",
                  "opprettet": "2025-01-10T09:00:00",
                  "varselSendt": "2025-01-12",
                  "behandlingsstatus": "TIL_BEHANDLING",
                  "totaltFeilutbetaltBelop": 25000,
                  "saksbehandlingURL": "https://tilbakekreving.intern.nav.no/behandling/$tilbakekrevingBehandlingId",
                  "fullstendigPeriode": {
                    "fom": "2025-01-01",
                    "tom": "2025-06-30"
                  }
                }
                """.trimIndent()
        }
    }

    @Test
    fun `Skal returnere nyeste TilbakekrevingHendelse fra tilstandsloggen`() {
        val eldste =
            lagTilbakekrevingHendelse(
                behandlingsstatus = TIL_BEHANDLING,
                opprettetTidspunkt = LocalDateTime.now().minusMinutes(1),
            )
        val nyeste =
            lagTilbakekrevingHendelse(
                behandlingsstatus = BehandlingStatus.TIL_GODKJENNING,
                opprettetTidspunkt = LocalDateTime.now(),
            )
        val oppgave =
            lagOppgave(
                tilstand = Oppgave.KlarTilKontroll,
                tilstandslogg =
                    OppgaveTilstandslogg(
                        Tilstandsendring(
                            tilstand = Oppgave.Tilstand.Type.KLAR_TIL_KONTROLL,
                            hendelse = nyeste,
                            tidspunkt = nyeste.hendelseOpprettet,
                        ),
                        Tilstandsendring(
                            tilstand = Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING,
                            hendelse = eldste,
                            tidspunkt = eldste.hendelseOpprettet,
                        ),
                    ),
                behandling =
                    lagBehandling(
                        behandlingId = tilbakekrevingBehandlingId,
                        utløstAvType = HendelseBehandler.Intern.Tilbakekreving,
                        hendelse = eldste,
                    ),
            )
        withTilbakekrevingApi(oppgaveMediatorSomReturnerer(oppgave)) {
            val response =
                client.get("tilbakekreving/$tilbakekrevingBehandlingId") {
                    autentisert()
                }
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldEqualSpecifiedJson
                //language=json
                """
                {
                  "behandlingsstatus": "TIL_GODKJENNING"
                }
                """.trimIndent()
        }
    }

    private fun oppgaveMediatorSomReturnerer(oppgave: Oppgave) =
        mockk<OppgaveMediator>().also {
            every {
                it.hentOppgaveForBehandling(behandlingId = tilbakekrevingBehandlingId, saksbehandler = any())
            } returns oppgave
        }

    private fun lagTilbakekrevingHendelse(
        behandlingsstatus: BehandlingStatus = TIL_BEHANDLING,
        opprettetTidspunkt: LocalDateTime = LocalDateTime.of(2025, 1, 10, 9, 0),
    ) = TilbakekrevingHendelse(
        eksternBehandlingId = UUIDv7.ny(),
        hendelseOpprettet = opprettetTidspunkt,
        tilbakekreving =
            Tilbakekreving(
                behandlingId = tilbakekrevingBehandlingId,
                opprettet = opprettetTidspunkt,
                avventBehandlingTilDato = null,
                varselSendt = LocalDate.of(2025, 1, 12),
                behandlingsstatus = behandlingsstatus,
                forrigeBehandlingsstatus = BehandlingStatus.TIL_FORHÅNDSVARSEL,
                totaltFeilutbetaltBeløp = BigDecimal("25000"),
                saksbehandlingURL = "https://tilbakekreving.intern.nav.no/behandling/$tilbakekrevingBehandlingId",
                fullstendigPeriode =
                    Tilbakekreving.Periode(
                        fom = LocalDate.of(2025, 1, 1),
                        tom = LocalDate.of(2025, 6, 30),
                    ),
            ),
    )

    private fun withTilbakekrevingApi(
        oppgaveMediator: OppgaveMediator,
        test: suspend ApplicationTestBuilder.() -> Unit,
    ) {
        testApplication {
            this.application {
                installerApis(
                    oppgaveMediator = oppgaveMediator,
                    oppgaveDTOMapper = mockk(),
                    produksjonsstatistikkRepository = mockk(),
                    klageMediator = mockk(),
                    klageDTOMapper = mockk(),
                    personMediator = mockk(),
                    sakMediator = mockk(),
                    innsendingMediator = mockk(),
                    meldingOmVedtakMediator = mockk(relaxed = true),
                    oppfølgingMediator = mockk(relaxed = true),
                    auditlogg = TestAuditlogg(),
                )
            }
            test()
        }
    }
}
