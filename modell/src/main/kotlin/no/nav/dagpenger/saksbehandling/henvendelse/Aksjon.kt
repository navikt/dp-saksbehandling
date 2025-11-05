package no.nav.dagpenger.saksbehandling.henvendelse

import java.util.UUID

sealed class Aksjon {
    object Avslutt : Aksjon()

    data class OpprettManuellBehandling(val saksbehandlerToken: String) : Aksjon()

    data class OpprettKlage(val sakId: UUID) : Aksjon()
}
