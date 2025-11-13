package no.nav.dagpenger.saksbehandling

class OppgaveTilstandslogg(
    tilstandsendringer: List<Tilstandsendring<RettTilDagpenger.Tilstand.Type>> = listOf(),
) : Tilstandslogg<RettTilDagpenger.Tilstand.Type>(tilstandsendringer.toMutableList()) {
    constructor(vararg tilstandsEndringer: Tilstandsendring<RettTilDagpenger.Tilstand.Type>) : this(tilstandsEndringer.toMutableList())
}
