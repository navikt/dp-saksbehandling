package no.nav.dagpenger.saksbehandling.generell

import java.time.LocalDate
import java.util.UUID

sealed class GenerellOppgaveAksjon {
    enum class Type {
        AVSLUTT,
        OPPRETT_MANUELL_BEHANDLING,
        OPPRETT_REVURDERING_BEHANDLING,
        OPPRETT_KLAGE,
        OPPRETT_GENERELL_OPPGAVE,
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

    data class OpprettRevurderingBehandling(
        val saksbehandlerToken: String,
        override val valgtSakId: UUID,
    ) : GenerellOppgaveAksjon() {
        override val type: Type = Type.OPPRETT_REVURDERING_BEHANDLING
    }

    data class OpprettKlage(
        override val valgtSakId: UUID,
    ) : GenerellOppgaveAksjon() {
        override val type: Type = Type.OPPRETT_KLAGE
    }

    data class OpprettGenerellOppgave(
        override val valgtSakId: UUID?,
        val tittel: String,
        val beskrivelse: String = "",
        val emneknagg: String,
        val frist: LocalDate? = null,
        val tildelSammeSaksbehandler: Boolean = false,
    ) : GenerellOppgaveAksjon() {
        override val type: Type = Type.OPPRETT_GENERELL_OPPGAVE
    }
}
