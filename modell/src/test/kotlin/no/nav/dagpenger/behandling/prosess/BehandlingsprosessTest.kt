package no.nav.dagpenger.behandling.prosess

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.Behandling
import no.nav.dagpenger.behandling.Person
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class BehandlingsprosessTest {
    private val testPerson = Person("123")

    @Test
    fun `Enkel prosess`() {
        val behandling = Behandling(testPerson, emptySet())
        val behandlingsprosess = Enkelprosess(behandling)

        behandlingsprosess.harNeste() shouldBe true
        behandlingsprosess.harTilbake() shouldBe false

        shouldNotThrow<IllegalStateException> {
            behandlingsprosess.neste()
        }

        behandlingsprosess.harNeste() shouldBe false
        behandlingsprosess.harTilbake() shouldBe false

        shouldThrow<IllegalStateException> {
            behandlingsprosess.tilbake()
        }
    }

    @Test
    @Disabled
    fun `Totrinnsprosess`() {
        val behandling = Behandling(testPerson, emptySet())
        val behandlingsprosess = Totrinnsprosess(behandling)

        behandlingsprosess.harNeste() shouldBe false
        behandlingsprosess.harTilbake() shouldBe false

        // behandling.utfall() = true

        behandlingsprosess.harNeste() shouldBe true
        behandlingsprosess.harTilbake() shouldBe false

        shouldNotThrow<IllegalStateException> {
            behandlingsprosess.neste()
        }

        behandlingsprosess.harNeste() shouldBe true
        behandlingsprosess.harTilbake() shouldBe true

        shouldNotThrow<IllegalStateException> {
            behandlingsprosess.tilbake()
        }

        behandlingsprosess.neste()

        behandlingsprosess.harNeste() shouldBe false
        behandlingsprosess.harTilbake() shouldBe false

        shouldThrow<IllegalStateException> {
            behandlingsprosess.tilbake()
        }
    }
}
