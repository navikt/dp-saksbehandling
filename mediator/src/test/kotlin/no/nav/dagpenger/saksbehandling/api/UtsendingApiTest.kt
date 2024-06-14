package no.nav.dagpenger.saksbehandling.api

import io.kotest.matchers.shouldBe
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.UtsendingMediator
import no.nav.dagpenger.saksbehandling.api.config.apiConfig
import no.nav.dagpenger.saksbehandling.utsending.hendelser.VedtaksbrevHendelse
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
    fun `Sending av brev går fint`() {
        val oppgaveId = UUIDv7.ny()
        val brev = "Html ja"
        val utsendingMediatorMock =
            mockk<UtsendingMediator>().also {
                every { it.mottaBrev(VedtaksbrevHendelse(oppgaveId, brev)) } just Runs
            }
        withUtsendingApi(utsendingMediatorMock) {
            val response =
                client.post("/utsending/$oppgaveId/send-brev") {
                    autentisert(token = gyldigToken)
                    setBody(brev)
                    contentType(ContentType.Text.Html)
                }
            response.status shouldBe HttpStatusCode.Accepted
        }
    }

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
            application {
                apiConfig()
                utsendingApi(utsendingMediator)
            }
            test()
        }
    }

    private fun HttpRequestBuilder.autentisert(token: String = gyldigToken) {
        header(HttpHeaders.Authorization, "Bearer $token")
    }
}
