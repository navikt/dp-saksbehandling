package no.nav.dagpenger.saksbehandling.klage

import no.nav.dagpenger.saksbehandling.Tilstandsendring
import no.nav.dagpenger.saksbehandling.Tilstandslogg

class KlageTilstandslogg(
    tilstandsendringer: List<Tilstandsendring<Klage.KlageTilstand.Type>> = listOf(),
) : Tilstandslogg<Klage.KlageTilstand.Type>(tilstandsendringer.toMutableList()) {
    constructor(vararg tilstandsEndringer: Tilstandsendring<Klage.KlageTilstand.Type>) : this(
        tilstandsEndringer.toMutableList(),
    )
}
