package no.nav.dagpenger.behandling.hendelser

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.Steg
import org.junit.jupiter.api.Test
import java.util.UUID

class SøknadInnsendtHendelseTest {
    private val behandling = SøknadInnsendtHendelse(UUID.randomUUID(), "123", "123").behandling()

    @Test
    fun `Ikke ferdig`() {
        behandling.erFerdig() shouldBe false
        behandling.utfall() shouldBe false
    }

    @Test
    fun `Ikke ferdig fordi vilkåret er oppfylt, men resten av stegene må utføres`() {
        (behandling.steg.single { it.id == "Oppfyller kravene til dagpenger" } as Steg.Vilkår).besvar(true)
        behandling.erFerdig() shouldBe false
        behandling.utfall() shouldBe true
    }

    @Test
    fun `Ferdig fordi vilkåret ikke er oppfylt`() {
        (behandling.steg.single { it.id == "Oppfyller kravene til dagpenger" } as Steg.Vilkår).besvar(false)
        behandling.erFerdig() shouldBe true
        behandling.utfall() shouldBe false
    }
}
