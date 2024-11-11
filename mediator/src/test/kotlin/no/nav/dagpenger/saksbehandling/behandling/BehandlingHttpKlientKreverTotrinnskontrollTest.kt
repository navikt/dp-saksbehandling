package no.nav.dagpenger.saksbehandling.behandling

import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.behandling.BehandlingHttpKlient.Companion.lagBehandlingHttpKlient
import org.junit.jupiter.api.Test
import java.util.UUID

class BehandlingHttpKlientKreverTotrinnskontrollTest {
    val saksbehandlerToken = "token"
    val behandlingId = UUID.fromString("019145eb-6fbb-769f-b1b1-d2450b383a98")
    val dpBehandlingApiUrl = "https://dp-behandling.intern.dev.nav.no/behandling"

    @Test
    fun `Test av request og parsing av response av behandling som krever totrinnskontroll`() {
        val resourseRetriever = object {}.javaClass
        val responseJson = resourseRetriever.getResource("/behandlingKreverIkkeTotrinnskontroll.json")!!.readText()
        val mockEngine =
            MockEngine { request ->
                request.headers["Authorization"] shouldBe "Bearer tulleToken"
                request.url.toString() shouldBe "$dpBehandlingApiUrl/$behandlingId"
                respond(
                    content = responseJson,
                    headers = headersOf("Content-Type", "application/json"),
                    status = HttpStatusCode.NoContent,
                )
            }

        val klient =
            BehandlingHttpKlient(
                dpBehandlingApiUrl = dpBehandlingApiUrl,
                tokenProvider = { "tulleToken" },
                httpClient =
                    lagBehandlingHttpKlient(
                        engine = mockEngine,
                        metricsBaseName = "hubba",
                    ),
            )

        runBlocking {
            klient.kreverTotrinnskontroll(behandlingId = behandlingId, saksbehandlerToken = saksbehandlerToken)
                .getOrThrow()
                .let { kreverTotrinnskontroll ->
                    kreverTotrinnskontroll shouldBe false
                }
        }
    }

    @Test
    fun `Test av request og parsing av response av behandling som ikke krever totrinnskontroll`() {
        val resourseRetriever = object {}.javaClass
        val responseJson = resourseRetriever.getResource("/behandlingKreverIkkeTotrinnskontroll.json")!!.readText()
        val mockEngine =
            MockEngine { request ->
                request.headers["Authorization"] shouldBe "Bearer tulleToken"
                request.url.toString() shouldBe "$dpBehandlingApiUrl/$behandlingId"
                respond(responseJson, headers = headersOf("Content-Type", "application/json"))
            }
        val klient =
            BehandlingHttpKlient(
                dpBehandlingApiUrl = dpBehandlingApiUrl,
                tokenProvider = { "tulleToken" },
                httpClient =
                    lagBehandlingHttpKlient(
                        engine = mockEngine,
                        metricsBaseName = "bubba",
                    ),
            )

        runBlocking {
            klient.kreverTotrinnskontroll(behandlingId = behandlingId, saksbehandlerToken = saksbehandlerToken)
                .getOrThrow()
                .let { kreverTotrinnskontroll ->
                    kreverTotrinnskontroll shouldBe false
                }
        }
    }

    @Test
    fun `Test av at sjekk om krever totrinnskontroll feiler`() {
        val behandlingIdSomFeiler = UUIDv7.ny()
        val mockEngine =
            MockEngine { request ->
                request.headers["Authorization"] shouldBe "Bearer tulleToken"
                request.url.toString() shouldBe "$dpBehandlingApiUrl/$behandlingIdSomFeiler"
                respond(
                    content = "Error",
                    status = HttpStatusCode.InternalServerError,
                )
            }

        val klient =
            BehandlingHttpKlient(
                dpBehandlingApiUrl = dpBehandlingApiUrl,
                tokenProvider = { "tulleToken" },
                httpClient =
                    lagBehandlingHttpKlient(
                        engine = mockEngine,
                        metricsBaseName = "kubba",
                    ),
            )

        runBlocking {
            klient.kreverTotrinnskontroll(
                behandlingId = behandlingIdSomFeiler,
                saksbehandlerToken = saksbehandlerToken,
            ).isFailure shouldBe true
        }
    }
}
