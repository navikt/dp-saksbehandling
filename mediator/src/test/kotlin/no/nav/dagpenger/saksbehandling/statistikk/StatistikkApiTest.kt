package no.nav.dagpenger.saksbehandling.statistikk

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.api.allApis
import no.nav.dagpenger.saksbehandling.api.mockAzure
import org.junit.jupiter.api.Test

class StatistikkApiTest {
    private val mockAzure = mockAzure()

    @Test
    fun `test public statistikk html response`() {
        val mockStatistikkTjeneste =
            mockk<StatistikkTjeneste>().also {
                every { it.hentAntallBrevSendt() } returns 3
            }
        testApplication {
            application {
                allApis(mockk(), mockk(), mockStatistikkTjeneste)
            }

            client.get("public/statistikk").let { httpResponse ->
                httpResponse.status.value shouldBe 200
                httpResponse.bodyAsText() shouldContain """Antall brev sendt: 3"""
            }
        }
    }
}
