package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.KlageOppgave.KlageOppgaveTilstand.KlarTilBehandling
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.PAA_VENT
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class KlageOppgave private constructor(
    override val oppgaveId: UUID,
    override val opprettet: LocalDateTime,
    override var behandlerIdent: String? = null,
    override val emneknagger: MutableSet<String>,
    override var utsattTil: LocalDate? = null,
    override val tilstandslogg: OppgaveTilstandslogg = OppgaveTilstandslogg(),
    override val person: Person,
    override val behandling: KlageBehandling,
    override var meldingOmVedtak: MeldingOmVedtak,
    override var tilstand: KlageOppgaveTilstand = KlarTilBehandling,
) : Oppgave() {
    constructor(
        oppgaveId: UUID,
        emneknagger: Set<String> = emptySet(),
        opprettet: LocalDateTime,
        tilstandType: Tilstand.Type = KLAR_TIL_BEHANDLING,
        behandlerIdent: String? = null,
        tilstandslogg: OppgaveTilstandslogg = OppgaveTilstandslogg(),
        person: Person,
        behandling: KlageBehandling,
        meldingOmVedtak: MeldingOmVedtak,
    ) : this(
        oppgaveId = oppgaveId,
        behandlerIdent = behandlerIdent,
        opprettet = opprettet,
        emneknagger = emneknagger.toMutableSet(),
        tilstandslogg = tilstandslogg,
        person = person,
        behandling = behandling,
        meldingOmVedtak = meldingOmVedtak,
    ) {
        this.tilstand =
            when (tilstandType) {
                KLAR_TIL_BEHANDLING -> KlarTilBehandling
                UNDER_BEHANDLING -> KlageOppgaveTilstand.UnderBehandling
                PAA_VENT -> KlageOppgaveTilstand.Påvent
                FERDIG_BEHANDLET -> KlageOppgaveTilstand.FerdigBehandlet
                else -> throw IllegalArgumentException("Ukjent tilstand for klageoppgave: $tilstandType")
            }
    }

    companion object {
        fun rehydrer(
            oppgaveId: UUID,
            behandlerIdent: String?,
            opprettet: LocalDateTime,
            emneknagger: Set<String>,
            tilstand: KlageOppgaveTilstand,
            utsattTil: LocalDate?,
            tilstandslogg: OppgaveTilstandslogg = OppgaveTilstandslogg(),
            person: Person,
            behandling: KlageBehandling,
            meldingOmVedtak: MeldingOmVedtak,
        ): KlageOppgave =
            KlageOppgave(
                oppgaveId = oppgaveId,
                opprettet = opprettet,
                behandlerIdent = behandlerIdent,
                emneknagger = emneknagger.toMutableSet(),
                tilstand = tilstand,
                utsattTil = utsattTil,
                tilstandslogg = tilstandslogg,
                person = person,
                behandling = behandling,
                meldingOmVedtak = meldingOmVedtak,
            )
    }

    sealed interface KlageOppgaveTilstand : Tilstand {
        object KlarTilBehandling : KlageOppgaveTilstand {
            override val type = KLAR_TIL_BEHANDLING
        }

        object UnderBehandling : KlageOppgaveTilstand {
            override val type = UNDER_BEHANDLING
        }

        object Påvent : KlageOppgaveTilstand {
            override val type = PAA_VENT
        }

        object FerdigBehandlet : KlageOppgaveTilstand {
            override val type = FERDIG_BEHANDLET
        }
    }
}
