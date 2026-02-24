package no.nav.dagpenger.saksbehandling

enum class ReturnerTilSaksbehandlingÅrsak {
    FEIL_UTFALL,
    FEIL_HJEMMEL,
    HAR_MANGLER,
    ANNET,
}

enum class FjernOppgaveAnsvarÅrsak {
    MANGLER_KOMPETANSE,
    INHABILITET,
    FRAVÆR,
    ANNET,
}
