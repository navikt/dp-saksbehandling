package no.nav.dagpenger.saksbehandling.klage

import no.nav.dagpenger.saksbehandling.Tilstandsendring
import no.nav.dagpenger.saksbehandling.Tilstandslogg

class KlageTilstandslogg(
    tilstandsendringer: List<Tilstandsendring<KlageBehandling.KlageTilstand.Type>> = mutableListOf(),
) : Tilstandslogg<KlageBehandling.KlageTilstand.Type>(tilstandsendringer.toMutableList()) {
    constructor(vararg tilstandsEndringer: Tilstandsendring<KlageBehandling.KlageTilstand.Type>) : this(
        tilstandsEndringer.toMutableList(),
    )
}
