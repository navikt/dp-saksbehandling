package no.nav.dagpenger.saksbehandling.generell

import java.util.UUID

sealed class GenerellOppgaveAksjon {
    enum class Type {
        AVSLUTT,
        OPPRETT_MANUELL_BEHANDLING,
        OPPRETT_KLAGE,
    }

    abstract val valgtSakId: UUID?
    abstract val type: Type

    data class Avslutt(
        override val valgtSakId: UUID?,
    ) : GenerellOppgaveAksjon() {
        override val type: Type = Type.AVSLUTT
    }

    data class OpprettManuellBehandling(
        val saksbehandlerToken: String,
        override val valgtSakId: UUID,
    ) : GenerellOppgaveAksjon() {
        override val type: Type = Type.OPPRETT_MANUELL_BEHANDLING
    }

    data class OpprettKlage(
        override val valgtSakId: UUID,
    ) : GenerellOppgaveAksjon() {
        override val type: Type = Type.OPPRETT_KLAGE
    }
}
