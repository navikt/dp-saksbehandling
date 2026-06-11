package no.nav.dagpenger.saksbehandling

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.behandling.BehandlingException
import org.junit.jupiter.api.Test

class ToNoeTest {
    @Test
    fun `to noe`() {
        BehandlingException(
            """{"nåværendeTilstand":"TilBeslutning","operasjon":"godkjenn"}""",
            409,
        ).toNoe() shouldBe Noe
    }

    fun ` feil to noe`() {
        BehandlingException(
            """{"hubba":"TilBeslutning","operasjon":"godkjenn"}""",
            409,
        ).toNoe() shouldBe null

        BehandlingException(
            "ikke json",
            409,
        ).toNoe() shouldBe null
    }
}
