package no.nav.dagpenger.saksbehandling

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.UUID

class SletteGamleOppgaverJobTest {
    private val testRapid = TestRapid()

    @Test
    fun `Sletter gamle oppgaver og sender avbryt_behandling event`() {
        runBlocking {
            val behandlingId1 = UUID.randomUUID()
            val behandlingId2 = UUID.randomUUID()
            val ident1 = "12345678901"
            val ident2 = "10987654321"

            val gamleOppgaverRepository =
                mockk<GamleOppgaverRepository>().also {
                    every { it.finnGamleOppgaver(any()) } returns
                        listOf(
                            GamleOppgaver(ident = ident1, behandlingId = behandlingId1),
                            GamleOppgaver(ident = ident2, behandlingId = behandlingId2),
                        )
                }

            SletteGamleOppgaverJob(
                rapidsConnection = testRapid,
                gamleOppgaverRepository = gamleOppgaverRepository,
            ).executeJob()

            testRapid.inspektør.size shouldBe 2
            testRapid.inspektør.message(0).let { jsonNode ->
                jsonNode["@event_name"].asText() shouldBe "avbryt_behandling"
                jsonNode["behandlingId"].asText() shouldBe behandlingId1.toString()
                jsonNode["ident"].asText() shouldBe ident1
            }

            testRapid.inspektør.message(1).let { jsonNode ->
                jsonNode["@event_name"].asText() shouldBe "avbryt_behandling"
                jsonNode["behandlingId"].asText() shouldBe behandlingId2.toString()
                jsonNode["ident"].asText() shouldBe ident2
            }
        }
    }
}
