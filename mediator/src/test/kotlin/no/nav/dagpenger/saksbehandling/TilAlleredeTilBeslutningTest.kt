package no.nav.dagpenger.saksbehandling

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.behandling.BehandlingException
import org.junit.jupiter.api.Test

class TilAlleredeTilBeslutningTest {
    @Test
    fun `to noe`() {
        BehandlingException(
            """{"nåværendeTilstand":"TilBeslutning","operasjon":"godkjenn"}""",
            409,
        ).tilAlleredeTilBeslutning() shouldBe AlleredeTilBeslutning
    }

    fun ` feil to noe`() {
        BehandlingException(
            """{"hubba":"TilBeslutning","operasjon":"godkjenn"}""",
            409,
        ).tilAlleredeTilBeslutning() shouldBe null

        BehandlingException(
            "ikke json",
            409,
        ).tilAlleredeTilBeslutning() shouldBe null
    }
}
