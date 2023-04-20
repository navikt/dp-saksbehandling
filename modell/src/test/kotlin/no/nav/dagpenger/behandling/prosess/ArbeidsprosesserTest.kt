package no.nav.dagpenger.behandling.prosess

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.Behandling
import no.nav.dagpenger.behandling.Person
import no.nav.dagpenger.behandling.Steg
import org.junit.jupiter.api.Test

class ArbeidsprosesserTest {
    @Test
    fun `Kan kjøre totrinnsprosess på behandling`() {
        val steg = Steg.Vilkår("foo")
        val behandling = Behandling(Person("123"), setOf(steg))
        val totrinnsprosess = Arbeidsprosesser().totrinnsprosess(behandling)

        totrinnsprosess.start("TilBehandling")
        // Kan ikke gå videre før behandlingen er ferdig
        totrinnsprosess.validTransitions() shouldBe listOf("VentPåMangelbrev")
        shouldThrow<IllegalStateException> {
            totrinnsprosess.transitionTo("Innstilt")
        }

        steg.besvar(true)
        totrinnsprosess.validTransitions() shouldBe listOf("Innstilt", "VentPåMangelbrev")
        totrinnsprosess.transitionTo("Innstilt")
        // Kan sendes tilbake til saksbehandler
        totrinnsprosess.transitionTo("TilBehandling")
        // Kan ikke hoppe fra TilBehandling direkte til vedtak  uten å gå via innstilt/beslutter
        shouldThrow<IllegalStateException> {
            totrinnsprosess.transitionTo("Vedtak")
        }

        shouldNotThrow<IllegalStateException> {
            totrinnsprosess.transitionTo("VentPåMangelbrev")
            totrinnsprosess.transitionTo("TilBehandling")
            totrinnsprosess.transitionTo("Innstilt")
            totrinnsprosess.transitionTo("Vedtak")
        }
    }
}
