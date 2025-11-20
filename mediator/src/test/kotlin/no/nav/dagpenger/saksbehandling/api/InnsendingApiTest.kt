package no.nav.dagpenger.saksbehandling.api

import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.innsending.InnsendingMediator
import org.junit.jupiter.api.Test

class InnsendingApiTest {
    init {
        mockAzure()
    }

    private val behandlingId = UUIDv7.ny()

    @Test
    fun `Skal kaste feil når det mangler autentisering`() {
        val mediator = mockk<InnsendingMediator>()
        withInnsendingApi(mediator) {
            client.get("innsending/$behandlingId").status shouldBe HttpStatusCode.Unauthorized
            client.put("innsending/$behandlingId/ferdigstill") {
                headers[HttpHeaders.ContentType] = "application/json"
                //language=json
                setBody("""{ "tullebody": "tull" }""".trimIndent())
            }.let { response ->
                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }
    }

    private fun withInnsendingApi(
        innsendingMediator: InnsendingMediator,
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
                    sakMediator = mockk(),
                    innsendingMediator = innsendingMediator,
                )
            }
            test()
        }
    }
}
