package no.nav.dagpenger.behandling.prosess

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.Behandling
import no.nav.dagpenger.behandling.Meldingsfabrikk.testPerson
import no.nav.dagpenger.behandling.Steg
import org.junit.jupiter.api.Test

class ArbeidsprosesserTest {
    @Test
    fun `Kan kjøre totrinnsprosess på behandling`() {
        val steg = Steg.Vilkår("foo")
        val behandling = Behandling(testPerson, setOf(steg))
        val totrinnsprosess = Arbeidsprosesser.totrinnsprosess(behandling)

        totrinnsprosess.start("TilBehandling")
        // Kan ikke gå videre før behandlingen er ferdig
        totrinnsprosess.muligeTilstander() shouldBe listOf("VentPåMangelbrev")
        shouldThrow<IllegalStateException> {
            totrinnsprosess.gåTil("Innstilt")
        }

        steg.besvar(true)
        totrinnsprosess.muligeTilstander() shouldBe listOf("Innstilt", "VentPåMangelbrev")
        totrinnsprosess.gåTil("Innstilt")
        // Kan sendes tilbake til saksbehandler
        totrinnsprosess.gåTil("TilBehandling")
        // Kan ikke hoppe fra TilBehandling direkte til vedtak  uten å gå via innstilt/beslutter
        shouldThrow<IllegalStateException> {
            totrinnsprosess.gåTil("Vedtak")
        }

        shouldNotThrow<IllegalStateException> {
            totrinnsprosess.gåTil("VentPåMangelbrev")
            totrinnsprosess.gåTil("TilBehandling")
            totrinnsprosess.gåTil("Innstilt")
            totrinnsprosess.gåTil("Vedtak")
        }
    }
}
