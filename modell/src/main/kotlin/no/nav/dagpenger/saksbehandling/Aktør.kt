package no.nav.dagpenger.saksbehandling

sealed class Aktør {
    data class Saksbehandler(val navIdent: String) : Aktør()

    data class Beslutter(val navIdent: String) : Aktør()

    data class System(val navn: String) : Aktør() {
        companion object {
            val dpSaksbehandling = System("dp-saksbehandling")
            val dpBehandling = System("dp-behandling")
        }
    }
}
