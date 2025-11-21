package no.nav.dagpenger.saksbehandling.innsending

import java.util.UUID

sealed class Aksjon {
    abstract val valgtSakId: UUID?

    data class Avslutt(override val valgtSakId: UUID?) : Aksjon()

    data class OpprettManuellBehandling(val saksbehandlerToken: String, override val valgtSakId: UUID) : Aksjon()

    data class OpprettKlage(override val valgtSakId: UUID) : Aksjon()
}
