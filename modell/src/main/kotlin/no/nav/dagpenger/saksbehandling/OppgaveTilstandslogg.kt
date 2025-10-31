package no.nav.dagpenger.saksbehandling

class OppgaveTilstandslogg(
    tilstandsendringer: List<Tilstandsendring<Oppgave.Tilstand.Type>> = listOf(),
) : Tilstandslogg<Oppgave.Tilstand.Type>(tilstandsendringer.toMutableList()) {
    constructor(vararg tilstandsEndringer: Tilstandsendring<Oppgave.Tilstand.Type>) : this(tilstandsEndringer.toMutableList())
}
