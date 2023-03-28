package no.nav.dagpenger.behandling.persistence

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class InmemoryTest {
    @Test
    fun hubba() {
        val behandling = Inmemory.hentBehandlinger().also {
            it.forEach { behandling ->
                behandling.steg.size shouldBe 10
                behandling.alleSteg().size shouldBe 19
            }
        }

        Inmemory.hentBehandling(behandling.first().uuid).let { behandling ->
            behandling.steg.size shouldBe 10
            behandling.alleSteg().size shouldBe 19
        }
    }
}
