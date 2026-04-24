package no.nav.dagpenger.saksbehandling
import org.junit.jupiter.api.Test

class SakHistorikkTest {
    @Test
    fun `SakHistorikk med ulike type saker`() {
        val søknadBehandling = ModellTestHelper.lagSøknadBehandling()
        val dagpengeSak = ModellTestHelper.lagSak()
        val sakHistorikk = ModellTestHelper.lagSakHistorikk(saker = setOf(
            Sak(
                sakId = TODO(),
                opprettet = TODO(),
                behandlinger = TODO()
            )
        ))
    }
}
