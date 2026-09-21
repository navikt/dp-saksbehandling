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
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.api.MockAzure.Companion.autentisert
import no.nav.dagpenger.saksbehandling.api.installerApis
import no.nav.dagpenger.saksbehandling.api.mockAzure
import no.nav.dagpenger.saksbehandling.audit.TestAuditlogg
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
    fun `Skal returnere tilbakekreving for gyldig behandlingId`() {
        val tilbakekrevingMediator =
            mockk<TilbakekrevingMediator>().also {
                every {
                    it.hent(behandlingId = tilbakekrevingBehandlingId, saksbehandler = any())
                } returns
                    TilbakekrevingMedPersonIdent(
                        personIdent = "12345678901",
                        tilbakekreving =
                            Tilbakekreving(
                                behandlingId = tilbakekrevingBehandlingId,
                                opprettet = LocalDateTime.of(2025, 1, 10, 9, 0),
                                varselSendt = LocalDate.of(2025, 1, 12),
                                behandlingsstatus = Tilbakekreving.BehandlingStatus.TIL_BEHANDLING,
                                totaltFeilutbetaltBeløp = BigDecimal(25000),
                                saksbehandlingURL =
                                    "https://tilbakekreving.intern.nav.no/behandling/$tilbakekrevingBehandlingId",
                                fullstendigPeriode =
                                    Tilbakekreving.Periode(
                                        fom = LocalDate.of(2025, 1, 1),
                                        tom = LocalDate.of(2025, 6, 30),
                                    ),
                                avventBehandlingTilDato = null,
                                forrigeBehandlingsstatus = null,
                            ),
                    )
            }

        withTilbakekrevingApi(tilbakekrevingMediator) {
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

    private fun withTilbakekrevingApi(
        tilbakekrevingMediator: TilbakekrevingMediator,
        test: suspend ApplicationTestBuilder.() -> Unit,
    ) {
        testApplication {
            this.application {
                installerApis(
                    tilbakekrevingMediator = tilbakekrevingMediator,
                    oppgaveMediator = mockk(),
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
