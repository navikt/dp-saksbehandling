package no.nav.dagpenger.saksbehandling.maskinell

import io.kotest.matchers.shouldNotBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.UUIDv7
import org.junit.jupiter.api.Test

class BehandlingKlientTest {
    private val testTokenProvider: (token: String) -> String = { _ -> "testToken" }
    private val baseUrl = "http://baseUrl"
    private val subjectToken = "gylidg_token"

    @Test
    fun `Skal hente json fra behandling`() {
        val mockEngine =
            MockEngine { request ->
                respond("{}")
            }
        val behandlingKlient =
            BehandlingKlient(
                behandlingUrl = baseUrl,
                tokenProvider = testTokenProvider,
                engine = mockEngine,
            )
        val behandling =
            runBlocking {
                behandlingKlient.hentBehandling(UUIDv7.ny(), subjectToken)
            }
        behandling shouldNotBe null
    }
}
