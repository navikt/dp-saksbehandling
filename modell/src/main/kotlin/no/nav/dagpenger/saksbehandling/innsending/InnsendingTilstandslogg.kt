package no.nav.dagpenger.saksbehandling.innsending

import no.nav.dagpenger.saksbehandling.Tilstandsendring
import no.nav.dagpenger.saksbehandling.Tilstandslogg
import no.nav.dagpenger.saksbehandling.innsending.Innsending.Tilstand

class InnsendingTilstandslogg(
    tilstandsendringer: List<Tilstandsendring<Tilstand.Type>> = listOf(),
) : Tilstandslogg<Tilstand.Type>(tilstandsendringer.toMutableList()) {
    constructor(vararg tilstandsendringer: Tilstandsendring<Tilstand.Type>) : this(tilstandsendringer.toMutableList())
}
