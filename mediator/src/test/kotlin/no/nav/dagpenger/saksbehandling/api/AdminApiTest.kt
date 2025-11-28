package no.nav.dagpenger.saksbehandling.api

import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.autentisert
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.gyldigAdminToken
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.gyldigMaskinToken
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.gyldigSaksbehandlerToken
import org.junit.jupiter.api.Test

class AdminApiTest {
    init {
        mockAzure()
    }

    @Test
    fun `Skal avvise kall uten gyldig admin token`() {
        withAdminApi {
            client.get(urlString = "admin/ping") { }.status shouldBe HttpStatusCode.Unauthorized
            client.get(urlString = "admin/ping") { autentisert(gyldigMaskinToken()) }.status shouldBe HttpStatusCode.Unauthorized
            client.get(urlString = "admin/ping") { autentisert(gyldigSaksbehandlerToken()) }.status shouldBe HttpStatusCode.Unauthorized

            client.get(urlString = "admin/ping") { autentisert(gyldigAdminToken()) }.status shouldBe HttpStatusCode.OK
        }
    }
}

private fun withAdminApi(test: suspend ApplicationTestBuilder.() -> Unit) {
    testApplication {
        application {
            installerApis(
                oppgaveMediator = mockk(),
                oppgaveDTOMapper = mockk(),
                statistikkTjeneste = mockk(relaxed = true),
                klageMediator = mockk(relaxed = true),
                klageDTOMapper = mockk(relaxed = true),
                personMediator = mockk(),
                sakMediator = mockk(relaxed = true),
            )
        }
        test()
    }
}
