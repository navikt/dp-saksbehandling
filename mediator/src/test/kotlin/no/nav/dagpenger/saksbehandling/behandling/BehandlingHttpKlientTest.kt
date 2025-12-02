package no.nav.dagpenger.saksbehandling.behandling

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.prometheus.metrics.model.registry.PrometheusRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.behandling.BehandlingHttpKlient.Companion.lagBehandlingHttpKlient
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class BehandlingHttpKlientTest {
    val saksbehandlerToken = "token"
    private val tokenProvider = { _: String -> saksbehandlerToken }
    private val ident = "testIdent"
    private val behandlingId = UUIDv7.ny()
    private val ukjentId = UUIDv7.ny()

    private var requestData: HttpRequestData? = null

    private fun behandlingKlient(
        delay: Duration = 0.seconds,
        timeOut: Duration = 200.milliseconds,
    ) = BehandlingHttpKlient(
        dpBehandlingApiUrl = "http://localhost",
        tokenProvider = tokenProvider,
        httpClient =
            lagBehandlingHttpKlient(
                timeOut = timeOut,
                engine =
                    MockEngine { request: HttpRequestData ->
                        delay(delay)
                        requestData = request
                        when (request.url.encodedPath) {
                            in setOf("/person/behandling") -> {
                                when (request.method.value) {
                                    "POST" -> {
                                        respond(
                                            //language=JSON
                                            content =
                                                """
                                                {
                                                  "behandlingId": "$behandlingId",
                                                  "kreverTotrinnskontroll": false
                                                }
                                                """.trimIndent(),
                                            status = HttpStatusCode.OK,
                                            headers = headersOf(HttpHeaders.ContentType, "application/json"),
                                        )
                                    }

                                    else -> {
                                        respond(
                                            content = "Error",
                                            status = HttpStatusCode.InternalServerError,
                                        )
                                    }
                                }
                            }

                            in
                            setOf(
                                "/$behandlingId/avbryt",
                                "/$behandlingId/godkjenn",
                                "/$behandlingId/beslutt",
                                "/$behandlingId/send-tilbake",
                            ),
                            -> {
                                respond(
                                    content = "OK",
                                    status = HttpStatusCode.OK,
                                )
                            }

                            else -> {
                                respond(
                                    content = "Error",
                                    status = HttpStatusCode.InternalServerError,
                                )
                            }
                        }
                    },
                registry = PrometheusRegistry(),
            ),
    )

    @Test
    fun `kall mot dp-behandling opprett manuelt behandling `() {
        runBlocking {
            val behandlingKlient = behandlingKlient()

            behandlingKlient.opprettManuellBehandling(ident, saksbehandlerToken)
                .getOrThrow() shouldBe behandlingId

            requireNotNull(requestData).let {
                it.body.contentType.toString() shouldBe "application/json"
                it.body.toByteArray().decodeToString() shouldEqualJson """{"ident":"$ident"}"""
                it.headers[HttpHeaders.Authorization] shouldBe "Bearer $saksbehandlerToken"
                it.headers[HttpHeaders.Accept] shouldBe "application/json"
            }

            behandlingKlient(
                delay = 20.milliseconds,
                timeOut = 10.milliseconds,
            ).opprettManuellBehandling(ident, saksbehandlerToken).isFailure
        }
    }

    @Test
    fun `kall mot dp-behandling happy path `(): Unit =
        runBlocking {
            val behandlingKlient = behandlingKlient()
            behandlingKlient.avbryt(behandlingId, ident, saksbehandlerToken).isSuccess shouldBe true
            requireNotNull(requestData).let {
                it.body.contentType.toString() shouldBe "application/json"
                it.body.toByteArray().decodeToString() shouldEqualJson """{"ident":"$ident"}"""
            }
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
    fun `error test når db-behandling ikke svarer`(): Unit =
        runBlocking {
            val behandlingKlient =
                behandlingKlient(
                    delay = 20.milliseconds,
                    timeOut = 10.milliseconds,
                )
            behandlingKlient.avbryt(behandlingId, ident, saksbehandlerToken).isFailure shouldBe true
            behandlingKlient.godkjenn(behandlingId, ident, saksbehandlerToken).isFailure shouldBe true
            behandlingKlient.beslutt(behandlingId, ident, saksbehandlerToken).isFailure shouldBe true
            behandlingKlient.sendTilbake(behandlingId, ident, saksbehandlerToken).isFailure shouldBe true
        }

    @Test
    fun `error test når db-behandling svarer med status kode 500`(): Unit =
        runBlocking {
            val behandlingKlient = behandlingKlient()
            behandlingKlient.avbryt(ukjentId, ident, saksbehandlerToken).isFailure shouldBe true
            behandlingKlient.godkjenn(ukjentId, ident, saksbehandlerToken).isFailure shouldBe true
            behandlingKlient.beslutt(ukjentId, ident, saksbehandlerToken).isFailure shouldBe true
            behandlingKlient.sendTilbake(ukjentId, ident, saksbehandlerToken).isFailure shouldBe true
        }
}
