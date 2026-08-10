package no.nav.dagpenger.saksbehandling.klage

import no.nav.dagpenger.saksbehandling.Tilstandsendring
import no.nav.dagpenger.saksbehandling.Tilstandslogg

class KlageTilstandslogg(
    tilstandsendringer: List<Tilstandsendring<KlageBehandling.KlageTilstand.Type>> = listOf(),
) : Tilstandslogg<KlageBehandling.KlageTilstand.Type>(tilstandsendringer.toMutableList()) {
    constructor(vararg tilstandsendringer: Tilstandsendring<KlageBehandling.KlageTilstand.Type>) : this(
        tilstandsendringer.toMutableList(),
    )
}
