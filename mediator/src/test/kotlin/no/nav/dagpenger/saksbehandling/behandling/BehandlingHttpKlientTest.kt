package no.nav.dagpenger.saksbehandling.behandling

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import io.prometheus.metrics.model.registry.PrometheusRegistry
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.behandling.BehandlingHttpKlient.Companion.lagBehandlingHttpKlient
import kotlin.test.Test

class BehandlingHttpKlientTest {
    val saksbehandlerToken = "token"
    private val tokenProvider = { _: String -> saksbehandlerToken }
    private val ident = "testIdent"
    private val behandlingId = UUIDv7.ny()
    private val ukjentId = UUIDv7.ny()

    private var requestData: HttpRequestData? = null

    private val behandlingKlient =
        BehandlingHttpKlient(
            dpBehandlingApiUrl = "http://localhost",
            tokenProvider = tokenProvider,
            httpClient =
                lagBehandlingHttpKlient(
                    engine =
                        MockEngine { request: HttpRequestData ->
                            requestData = request
                            when (request.url.encodedPath) {
                                in setOf("/$behandlingId/godkjenn", "/$behandlingId/beslutt", "/$behandlingId/send-tilbake") ->
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

    @Test
    fun `kall mot dp-behandling happy path `(): Unit =
        runBlocking {
            behandlingKlient.godkjenn(behandlingId, ident, saksbehandlerToken).isSuccess shouldBe true
            requireNotNull(requestData).let {
                it.body.contentType.toString() shouldBe "application/json"
                it.body.toByteArray().decodeToString() shouldEqualJson """{"ident":"$ident"}"""
            }

            behandlingKlient.beslutt(behandlingId, ident, saksbehandlerToken).isSuccess shouldBe true
            requireNotNull(requestData).let {
                it.body.contentType.toString() shouldBe "application/json"
                it.body.toByteArray().decodeToString() shouldEqualJson """{"ident":"$ident"}"""
            }

            behandlingKlient.sendTilbake(behandlingId, ident, saksbehandlerToken).isSuccess shouldBe true
            requireNotNull(requestData).let {
                it.body.contentType.toString() shouldBe "application/json"
                it.body.toByteArray().decodeToString() shouldEqualJson """{"ident":"$ident"}"""
            }
        }

    @Test
    fun `godkjennBehandling error test`(): Unit =
        runBlocking {
            behandlingKlient.godkjenn(ukjentId, ident, saksbehandlerToken).isFailure shouldBe true
            behandlingKlient.beslutt(ukjentId, ident, saksbehandlerToken).isFailure shouldBe true
            behandlingKlient.sendTilbake(ukjentId, ident, saksbehandlerToken).isFailure shouldBe true
        }
}
