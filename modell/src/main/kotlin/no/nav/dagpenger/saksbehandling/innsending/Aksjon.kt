package no.nav.dagpenger.saksbehandling.innsending

import java.util.UUID

sealed class Aksjon {
    enum class Type {
        AVSLUTT,
        OPPRETT_MANUELL_BEHANDLING,
        OPPRETT_KLAGE,
    }

    abstract val valgtSakId: UUID?
    abstract val type: Type

    data class Avslutt(override val valgtSakId: UUID?) : Aksjon() {
        override val type: Type = Type.AVSLUTT
    }

    data class OpprettManuellBehandling(val saksbehandlerToken: String, override val valgtSakId: UUID) : Aksjon() {
        override val type: Type = Type.OPPRETT_MANUELL_BEHANDLING
    }

    data class OpprettKlage(override val valgtSakId: UUID) : Aksjon() {
        override val type: Type = Type.OPPRETT_KLAGE
    }
}
