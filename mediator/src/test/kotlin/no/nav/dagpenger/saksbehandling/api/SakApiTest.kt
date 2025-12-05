package no.nav.dagpenger.saksbehandling.api

import io.kotest.assertions.json.shouldEqualSpecifiedJson
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.api.MockAzure.Companion.gyldigMaskinToken
import no.nav.dagpenger.saksbehandling.db.oppgave.DataNotFoundException
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import org.junit.jupiter.api.Test

class SakApiTest {
    init {
        mockAzure()
    }

    private val behandlingId = UUIDv7.ny()
    private val sakId = UUIDv7.ny()

    @Test
    fun `Skal kaste feil når det mangler autentisering`() {
        withSakApi(mockk<SakMediator>()) {
            client.get("behandling/$behandlingId/sakId").status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `Skal kunne hente ut sakId for en behandlingId`() {
        val sakMediator =
            mockk<SakMediator>().also {
                every { it.hentDagpengerSakIdForBehandlingId(behandlingId) } returns sakId
            }
        val token = gyldigMaskinToken()
        withSakApi(sakMediator) {
            client
                .get("behandling/$behandlingId/sakId") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }.bodyAsText() shouldBe sakId.toString()
        }
    }

    @Test
    fun `404 med http problem hvis sakId ikke finnes`() {
        val sakMediator =
            mockk<SakMediator>().also {
                every { it.hentDagpengerSakIdForBehandlingId(any()) } throws DataNotFoundException("Fant ikke sakId for behandling")
            }
        val token = gyldigMaskinToken()
        withSakApi(sakMediator) {
            client
                .get("behandling/$behandlingId/sakId") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }.let {
                    it.status shouldBe HttpStatusCode.NotFound
                    //language=Json
                    it.bodyAsText() shouldEqualSpecifiedJson
                        """
                        {
                          "type": "dagpenger.nav.no/saksbehandling:problem:ressurs-ikke-funnet",
                          "title": "Ressurs ikke funnet",
                          "status": 404,
                          "detail": "Fant ikke sakId for behandling",
                          "instance": "/behandling/$behandlingId/sakId"
                        }
                        """.trimIndent()
                }
        }
    }

    private fun withSakApi(
        sakMediator: SakMediator,
        test: suspend ApplicationTestBuilder.() -> Unit,
    ) {
        testApplication {
            this.application {
                installerApis(
                    oppgaveMediator = mockk(),
                    oppgaveDTOMapper = mockk(),
                    statistikkTjeneste = mockk(),
                    klageMediator = mockk(),
                    klageDTOMapper = mockk(),
                    personMediator = mockk(),
                    sakMediator = sakMediator,
                    innsendingMediator = mockk(),
                )
            }
            test()
        }
    }
}
