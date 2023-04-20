package no.nav.dagpenger.behandling.prosess

import no.nav.dagpenger.behandling.Behandling

class Arbeidsprosesser {
    fun totrinnsprosess(behandling: Behandling) = Arbeidsprosess().apply {
        leggTilTilstand(
            "TilBehandling",
            listOf(
                Arbeidsprosess.Overgang("Innstilt") { behandling.erFerdig() },
                Arbeidsprosess.Overgang("VentPåMangelbrev"),
            ),
        )
        leggTilTilstand(
            "Innstilt",
            listOf(
                Arbeidsprosess.Overgang("TilBehandling"),
                Arbeidsprosess.Overgang("Vedtak"),
            ),
        )
        leggTilTilstand("VentPåMangelbrev", listOf(Arbeidsprosess.Overgang("TilBehandling")))
        leggTilTilstand("Vedtak", listOf())
    }
}
