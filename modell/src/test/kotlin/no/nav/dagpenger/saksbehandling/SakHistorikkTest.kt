package no.nav.dagpenger.saksbehandling
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SakHistorikkTest {
    @Test
    fun `SakHistorikk med ulike type saker`() {
        val sakHistorikk = ModellTestHelper.lagSakHistorikk()
        sakHistorikk.ferietilleggSaker() shouldBe listOf(ModellTestHelper.ferietilleggSak)
        sakHistorikk.dagpengeSaker() shouldBe listOf(ModellTestHelper.dagpengeSak)
    }
}
