package no.nav.dagpenger.saksbehandling.api

import io.kotest.matchers.shouldBe
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.UtsendingMediator
import org.junit.jupiter.api.Test

class UtsendingApiTest {
    private val testNAVIdent = "Z999999"
    private val mockAzure = mockAzure()
    private val gyldigToken =
        mockAzure.lagTokenMedClaims(
            mapOf(
                "groups" to listOf("SaksbehandlerADGruppe"),
                "NAVident" to testNAVIdent,
            ),
        )

    @Test
    fun `Skal avvise kall uten autoriserte AD grupper`() {
        withUtsendingApi {
            client.post(
                "/utsending/${UUIDv7.ny()}/send-brev",
            ) { autentisert(token = mockAzure.lagTokenMedClaims(mapOf("groups" to "UgyldigADGruppe"))) }
                .status shouldBe HttpStatusCode.Unauthorized
        }
    }

    private fun withUtsendingApi(
        utsendingMediator: UtsendingMediator = mockk<UtsendingMediator>(relaxed = true),
        test: suspend ApplicationTestBuilder.() -> Unit,
    ) {
        testApplication {
            application { utsendingApi(utsendingMediator) }
            test()
        }
    }

    private fun HttpRequestBuilder.autentisert(token: String = gyldigToken) {
        header(HttpHeaders.Authorization, "Bearer $token")
    }
}
