package no.nav.dagpenger.saksbehandling

enum class ReturnerTilSaksbehandlingÅrsak {
    FEIL_UTFALL,
    FEIL_HJEMMEL,
    HAR_MANGLER,
    ANNET,
}

enum class FjernOppgaveAnsvarÅrsak(
    val visningsnavn: String,
) {
    MANGLER_KOMPETANSE("Mangler kompetanse"),
    INHABILITET("Inhabilitet"),
    FRAVÆR("Fravær"),
    ANNET("Annet"),
}
