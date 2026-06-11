package no.nav.dagpenger.saksbehandling

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.behandling.BehandlingException
import org.junit.jupiter.api.Test

class TilAlleredeTilBeslutningTest {
    @Test
    fun `kan parse gyldig json`() {
        BehandlingException(
            """{"nåværendeTilstand":"TilBeslutning","operasjon":"godkjenn"}""",
            409,
        ).tilAlleredeTilBeslutning() shouldBe AlleredeTilBeslutning
    }

    @Test
    fun `null  hvis ugyldig jsoni, ingen json eller manglende felter `() {
        BehandlingException(
            """{"hubba":"TilBeslutning","operasjon":"godkjenn"}""",
            409,
        ).tilAlleredeTilBeslutning() shouldBe null

        BehandlingException(
            "ikke json",
            409,
        ).tilAlleredeTilBeslutning() shouldBe null

        BehandlingException(
            """{"nåværendeTilstand":"hubba","operasjon":"godkjenn"}""",
            409,
        ).tilAlleredeTilBeslutning() shouldBe null
        BehandlingException(
            null,
            409,
        ).tilAlleredeTilBeslutning() shouldBe null
    }
}
