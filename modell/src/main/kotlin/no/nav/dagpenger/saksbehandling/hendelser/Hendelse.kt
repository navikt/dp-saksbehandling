package no.nav.dagpenger.saksbehandling.hendelser

sealed class Hendelse(
    val aktør: Aktør = Aktør.Ukjent,
)

data object TomHendelse : Hendelse()

sealed class Aktør {
    data object Ukjent : Aktør()

    data class Saksbehandler(val navIdent: String) : Aktør()

    data class System(val navn: String) : Aktør()
}
