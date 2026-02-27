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

enum class KvalitetskontrollÅrsak(
    val visningsnavn: String,
) {
    OPPLÆRING("Opplæring"),
    INNGRIPENDE_FOR_BRUKER("Inngripende for bruker"),
    KOMPLISERT_VURDERING("Komplisert vurdering"),
    SKJØNNSMESSIG_VURDERING("Skjønnsmessig vurdering"),
    ANNET("Annet"),
    TOTRINNSKONTROLL("Totrinnskontroll"),
}
