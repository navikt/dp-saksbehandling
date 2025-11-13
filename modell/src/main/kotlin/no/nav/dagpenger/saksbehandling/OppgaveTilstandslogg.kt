package no.nav.dagpenger.saksbehandling

class OppgaveTilstandslogg(
    tilstandsendringer: List<Tilstandsendring<RettTilDagpengerOppgave.Tilstand.Type>> = listOf(),
) : Tilstandslogg<RettTilDagpengerOppgave.Tilstand.Type>(tilstandsendringer.toMutableList()) {
    constructor(
        vararg tilstandsEndringer: Tilstandsendring<RettTilDagpengerOppgave.Tilstand.Type>,
    ) : this(tilstandsEndringer.toMutableList())
}
