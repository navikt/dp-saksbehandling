package no.nav.dagpenger.saksbehandling.api

import io.kotest.assertions.json.shouldEqualSpecifiedJson
import io.kotest.assertions.json.shouldEqualSpecifiedJsonIgnoringOrder
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.autentisert
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.gyldigMaskinToken
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
            client.request("/sak/siste-sak-id/for-ident") {
                method = HttpMethod.Post
            }.status shouldBe HttpStatusCode.Unauthorized
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
            client.get("behandling/$behandlingId/sakId") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }.bodyAsText() shouldBe sakId.toString()
        }
    }

    @Test
    fun `Skal hente sakId for siste sak hvis bruker har noen saker`() {
        val identMedSaker = "12345612345"
        val identUtenSaker = "01010112345"
        val sakId = UUIDv7.ny()
        val sakMediator =
            mockk<SakMediator>().also {
                every { it.finnSisteSakId(identMedSaker) } returns sakId
                every { it.finnSisteSakId(identUtenSaker) } returns null
            }
        withSakApi(
            sakMediator = sakMediator,
        ) {
            client.post("sak/siste-sak-id/for-ident") {
                header(HttpHeaders.Authorization, "Bearer ${gyldigMaskinToken()}")
                contentType(ContentType.Application.Json)
                setBody(
                    //language=JSON
                    """{"ident": "$identMedSaker"}
                    """.trimMargin(),
                )
            }.also { response ->
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldEqualSpecifiedJsonIgnoringOrder
                    //language=JSON
                    """
                    {
                      "id" : "$sakId"
                    }
                    """.trimIndent()
            }
            client.post("sak/siste-sak-id/for-ident") {
                autentisert(token = gyldigMaskinToken())
                contentType(ContentType.Application.Json)
                setBody(
                    //language=JSON
                    """{"ident": "$identUtenSaker"}
                    """.trimMargin(),
                )
            }.also { response ->
                response.status shouldBe HttpStatusCode.NoContent
            }
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
            client.get("behandling/$behandlingId/sakId") {
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
                )
            }
            test()
        }
    }
}
