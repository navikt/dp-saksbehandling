package no.nav.dagpenger.saksbehandling.tilbakekreving

import io.kotest.assertions.json.shouldEqualSpecifiedJson
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.OppgaveTilstandslogg
import no.nav.dagpenger.saksbehandling.TestHelper.testPerson
import no.nav.dagpenger.saksbehandling.Tilstandsendring
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.UtløstAvType
import no.nav.dagpenger.saksbehandling.api.MockAzure.Companion.autentisert
import no.nav.dagpenger.saksbehandling.api.installerApis
import no.nav.dagpenger.saksbehandling.api.mockAzure
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
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
    fun `Skal returnere 401 uten autentisering`() {
        withTilbakekrevingApi(mockk(relaxed = true)) {
            client.get("tilbakekreving/$tilbakekrevingBehandlingId").status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `Skal returnere 404 når oppgave ikke finnes`() {
        val oppgaveRepository =
            mockk<OppgaveRepository>().also {
                every { it.finnOppgaveFor(any()) } returns null
            }
        withTilbakekrevingApi(oppgaveRepository) {
            client
                .get("tilbakekreving/$tilbakekrevingBehandlingId") {
                    autentisert()
                }.status shouldBe HttpStatusCode.NotFound
        }
    }

    @Test
    fun `Skal returnere 404 når oppgave ikke er tilbakekreving`() {
        val oppgave =
            Oppgave.rehydrer(
                oppgaveId = UUIDv7.ny(),
                behandlerIdent = null,
                opprettet = LocalDateTime.now(),
                emneknagger = emptySet(),
                tilstand = Oppgave.KlarTilBehandling,
                utsattTil = null,
                person = testPerson,
                behandling =
                    Behandling(
                        behandlingId = tilbakekrevingBehandlingId,
                        opprettet = LocalDateTime.now(),
                        utløstAv = UtløstAvType.SØKNAD,
                        hendelse = mockk(),
                    ),
                meldingOmVedtak =
                    Oppgave.MeldingOmVedtak(
                        kilde = Oppgave.MeldingOmVedtakKilde.DP_SAK,
                        kontrollertGosysBrev = Oppgave.KontrollertBrev.IKKE_RELEVANT,
                    ),
            )
        val oppgaveRepository =
            mockk<OppgaveRepository>().also {
                every { it.finnOppgaveFor(tilbakekrevingBehandlingId) } returns oppgave
            }
        withTilbakekrevingApi(oppgaveRepository) {
            client
                .get("tilbakekreving/$tilbakekrevingBehandlingId") {
                    autentisert()
                }.status shouldBe HttpStatusCode.NotFound
        }
    }

    @Test
    fun `Skal returnere tilbakekreving for gyldig behandlingId`() {
        val hendelse = lagTilbakekrevingHendelse()
        val oppgave = lagOppgaveMedTilbakekreving(hendelse)
        val oppgaveRepository =
            mockk<OppgaveRepository>().also {
                every { it.finnOppgaveFor(tilbakekrevingBehandlingId) } returns oppgave
            }
        withTilbakekrevingApi(oppgaveRepository) {
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
                  "sakOpprettet": "2025-01-10T09:00:00",
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

    private fun lagTilbakekrevingHendelse() =
        TilbakekrevingHendelse(
            ident = "12345678910",
            eksternFagsakId = "100001234",
            eksternBehandlingId = UUIDv7.ny(),
            hendelseOpprettet = LocalDateTime.of(2025, 1, 15, 10, 0),
            tilbakekreving =
                TilbakekrevingHendelse.Tilbakekreving(
                    behandlingId = tilbakekrevingBehandlingId,
                    sakOpprettet = LocalDateTime.of(2025, 1, 10, 9, 0),
                    varselSendt = LocalDate.of(2025, 1, 12),
                    behandlingsstatus = TilbakekrevingHendelse.BehandlingStatus.TIL_BEHANDLING,
                    forrigeBehandlingsstatus = TilbakekrevingHendelse.BehandlingStatus.OPPRETTET,
                    totaltFeilutbetaltBeløp = BigDecimal("25000"),
                    saksbehandlingURL = "https://tilbakekreving.intern.nav.no/behandling/$tilbakekrevingBehandlingId",
                    fullstendigPeriode =
                        TilbakekrevingHendelse.Periode(
                            fom = LocalDate.of(2025, 1, 1),
                            tom = LocalDate.of(2025, 6, 30),
                        ),
                ),
        )

    private fun lagOppgaveMedTilbakekreving(hendelse: TilbakekrevingHendelse): Oppgave =
        Oppgave.rehydrer(
            oppgaveId = UUIDv7.ny(),
            behandlerIdent = null,
            opprettet = LocalDateTime.now(),
            emneknagger = emptySet(),
            tilstand = Oppgave.KlarTilBehandling,
            utsattTil = null,
            tilstandslogg =
                OppgaveTilstandslogg(
                    Tilstandsendring(
                        tilstand = Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING,
                        hendelse = hendelse,
                    ),
                ),
            person = testPerson,
            behandling =
                Behandling(
                    behandlingId = tilbakekrevingBehandlingId,
                    opprettet = hendelse.hendelseOpprettet,
                    utløstAv = UtløstAvType.TILBAKEKREVING,
                    hendelse = hendelse,
                ),
            meldingOmVedtak =
                Oppgave.MeldingOmVedtak(
                    kilde = Oppgave.MeldingOmVedtakKilde.DP_SAK,
                    kontrollertGosysBrev = Oppgave.KontrollertBrev.IKKE_RELEVANT,
                ),
        )

    private fun withTilbakekrevingApi(
        oppgaveRepository: OppgaveRepository,
        test: suspend io.ktor.server.testing.ApplicationTestBuilder.() -> Unit,
    ) {
        testApplication {
            this.application {
                installerApis(
                    oppgaveMediator = mockk(),
                    oppgaveDTOMapper = mockk(),
                    produksjonsstatistikkRepository = mockk(),
                    klageMediator = mockk(),
                    klageDTOMapper = mockk(),
                    personMediator = mockk(),
                    sakMediator = mockk(),
                    innsendingMediator = mockk(),
                    meldingOmVedtakMediator = mockk(relaxed = true),
                    oppgaveRepository = oppgaveRepository,
                )
            }
            test()
        }
    }
}
