package no.nav.dagpenger.saksbehandling.klage

import no.nav.dagpenger.saksbehandling.hendelser.FerdigstillKlageOppgave
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import java.time.LocalDateTime
import java.util.UUID

// TODO: SLETTES - denne har sannsynligvis ikke livets rett. Oppgave er generisk i seg selv.
class KlageOppgave(
    oppgaveId: UUID,
    opprettet: LocalDateTime,
    private var journalpostId: String? = null,
    val klageBehandling: KlageBehandling,
) : GeneriskOppgave(
        oppgaveId = oppgaveId,
        opprettet = opprettet,
        _emneknagger = mutableSetOf(),
    ) {
    private var tilstand: Tilstand = KlarTilBehandling

    fun tilstand() = this.tilstand

    fun tildel(settOppgaveAnsvarHendelse: SettOppgaveAnsvarHendelse) {
        tilstand.tildel(this, settOppgaveAnsvarHendelse)
    }

    fun ferdigstill(ferdigstillKlageOppgave: FerdigstillKlageOppgave) {
        tilstand.ferdigstill(this, ferdigstillKlageOppgave)
    }

    sealed interface Tilstand {
        fun tildel(
            klageOppgave: KlageOppgave,
            settOppgaveAnsvarHendelse: SettOppgaveAnsvarHendelse,
        ) {
            throw IllegalStateException("Kan ikke tildele oppgave i denne tilstanden")
        }

        fun ferdigstill(
            klageOppgave: KlageOppgave,
            settOppgaveAnsvarHendelse: FerdigstillKlageOppgave,
        ) {
            throw IllegalStateException("Kan ikke ferdigstille oppgave i denne tilstanden")
        }

        val type: Type

        enum class Type {
            KLAGE_KLAR_TIL_BEHANDLING,
            KLAGE_UNDER_BEHANDLING,
            KLAGE_FERDIG_BEHANDLET,
        }
    }

    data object KlarTilBehandling : Tilstand {
        override val type: Tilstand.Type = Tilstand.Type.KLAGE_KLAR_TIL_BEHANDLING

        override fun tildel(
            klageOppgave: KlageOppgave,
            settOppgaveAnsvarHendelse: SettOppgaveAnsvarHendelse,
        ) {
            klageOppgave.tilstand = UnderBehandling
        }
    }

    data object UnderBehandling : Tilstand {
        override val type: Tilstand.Type = Tilstand.Type.KLAGE_UNDER_BEHANDLING

        override fun ferdigstill(
            klageOppgave: KlageOppgave,
            settOppgaveAnsvarHendelse: FerdigstillKlageOppgave,
        ) {
            klageOppgave.tilstand = FerdigBehandlet
        }
    }

    data object FerdigBehandlet : Tilstand {
        override val type: Tilstand.Type = Tilstand.Type.KLAGE_FERDIG_BEHANDLET
    }
}
