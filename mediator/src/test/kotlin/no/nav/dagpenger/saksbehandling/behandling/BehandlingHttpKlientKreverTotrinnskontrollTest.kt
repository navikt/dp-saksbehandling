package no.nav.dagpenger.saksbehandling.behandling

import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.behandling.BehandlngHttpKlient.Companion.lagBehandlingHttpKlient
import org.junit.jupiter.api.Test
import java.util.UUID

class BehandlingHttpKlientKreverTotrinnskontrollTest {

    val saksbehandlerToken = "token"
    val behandlingId = UUID.fromString("019145eb-6fbb-769f-b1b1-d2450b383a98")
    val dpBehandlingApiUrl = "https://dp-behandling.intern.dev.nav.no/behandling"

    @Test
    fun `Test av request og parsing av response av behandling som krever totrinnskontroll`() {
        val resourseRetriever = object {}.javaClass
        val responseJson = resourseRetriever.getResource("/behandlingKreverIkkeTotrinnskontroll.json").readText()
        val mockEngine =
            MockEngine { request ->
                request.headers["Authorization"] shouldBe "Bearer tulleToken"
                request.url.toString() shouldBe "$dpBehandlingApiUrl/$behandlingId"
                respond(responseJson, headers = headersOf("Content-Type", "application/json"))
            }
        val klient =
            BehandlngHttpKlient(
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
        val saksbehandlerToken = "token"
        val behandlingId = UUID.fromString("019145eb-6fbb-769f-b1b1-d2450b383a98")
        val dpBehandlingApiUrl = "https://dp-behandling.intern.dev.nav.no/behandling"
        val responseJson = resourseRetriever.getResource("/behandlingKreverIkkeTotrinnskontroll.json").readText()
        val mockEngine =
            MockEngine { request ->
                request.headers["Authorization"] shouldBe "Bearer tulleToken"
                request.url.toString() shouldBe "$dpBehandlingApiUrl/$behandlingId"
                respond(responseJson, headers = headersOf("Content-Type", "application/json"))
            }
        val klient =
            BehandlngHttpKlient(
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
}
