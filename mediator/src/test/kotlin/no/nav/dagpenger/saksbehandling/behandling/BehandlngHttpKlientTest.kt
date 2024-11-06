package no.nav.dagpenger.saksbehandling.behandling

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.prometheus.metrics.model.registry.PrometheusRegistry
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.behandling.BehandlngHttpKlient.Companion.lagBehandlingHttpKlient
import java.util.UUID
import kotlin.test.Test

class BehandlngHttpKlientTest {
    val saksbehandlerToken = "token"
    private val tokenProvider = { _: String -> saksbehandlerToken }
    private val ident = "testIdent"
    private val behandlingId = UUIDv7.ny()

    private var requestData: HttpRequestData? = null

    val mockEngineKreverTotrinns =
        MockEngine { request ->
            request.headers[HttpHeaders.Authorization] shouldBe "Bearer token"
            request.url.host shouldBe "localhost"
            request.url.pathSegments shouldContain (behandlingId.toString())
            respond(
                content =
                //language=JSON
                """
                    {
                        "behandlingId": "$behandlingId",
                        "kreverTotrinnskontroll" : true 
                    }
                """.trimIndent(),
                headers = headersOf("Content-Type", "json"),
            )
        }

    private val behandlingKlient =
        BehandlngHttpKlient(
            dpBehandlingApiUrl = "http://localhost",
            tokenProvider = tokenProvider,
            httpClient =
                lagBehandlingHttpKlient(
                    engine =
                        MockEngine { request: HttpRequestData ->
                            requestData = request
                            when (request.url.encodedPath) {
                                "/$behandlingId/godkjenn" ->
                                    respond(
                                        content = "OK",
                                        status = HttpStatusCode.OK,
                                    )

                                else ->
                                    respond(
                                        content = "Error",
                                        status = HttpStatusCode.InternalServerError,
                                    )
                            }
                        },
                    registry = PrometheusRegistry(),
                ),
        )

    private val behandlingKlientKreverTotrinns =
        BehandlngHttpKlient(
            dpBehandlingApiUrl = "http://localhost",
            tokenProvider = tokenProvider,
            httpClient =
                lagBehandlingHttpKlient(
                    engine = mockEngineKreverTotrinns,
                    registry = PrometheusRegistry(),
                ),
        )

    @Test
    fun `godkjennBehandling should return success`(): Unit =
        runBlocking {
            behandlingKlient.godkjennBehandling(behandlingId, ident, saksbehandlerToken).isSuccess shouldBe true
            requireNotNull(requestData).let {
                it.body.contentType.toString() shouldBe "application/json"
                it.body.toByteArray().decodeToString() shouldEqualJson """{"ident":"$ident"}"""
            }
        }

    @Test
    fun `godkjennBehandling error test`(): Unit =
        runBlocking {
            behandlingKlient.godkjennBehandling(UUID.randomUUID(), ident, saksbehandlerToken).isFailure shouldBe true
        }

    @Test
    fun `hentTotrinnskontroll skal svare true`() {
        runBlocking {
            behandlingKlientKreverTotrinns.kreverTotrinnskontroll(behandlingId, saksbehandlerToken).isSuccess shouldBe true
            behandlingKlientKreverTotrinns.kreverTotrinnskontroll(behandlingId, saksbehandlerToken).let {
                it.onSuccess { response ->
                    response shouldBe true
                }
                it.onFailure { true shouldBe false }
            }
        }
    }
}
