package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.RettTilDagpengerOppgave.RettTilDagpengerTilstand
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class KlageOppgave private constructor(
    override val oppgaveId: UUID,
    override val opprettet: LocalDateTime,
    override var behandlerIdent: String? = null,
    override val emneknagger: MutableSet<String>,
    override var tilstand: RettTilDagpengerTilstand = KlarTilBehandling,
    override var utsattTil: LocalDate? = null,
    override val tilstandslogg: OppgaveTilstandslogg = OppgaveTilstandslogg(),
    override val person: Person,
    override val behandling: KlageBehandling,
    override var meldingOmVedtak: MeldingOmVedtak,
) : Oppgave() {
    constructor(
        oppgaveId: UUID,
        emneknagger: Set<String> = emptySet(),
        opprettet: LocalDateTime,
        tilstand: RettTilDagpengerTilstand = KlarTilBehandling,
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
        tilstand = tilstand,
        tilstandslogg = tilstandslogg,
        person = person,
        behandling = behandling,
        meldingOmVedtak = meldingOmVedtak,
    )

    companion object {
        fun rehydrer(
            oppgaveId: UUID,
            behandlerIdent: String?,
            opprettet: LocalDateTime,
            emneknagger: Set<String>,
            tilstand: RettTilDagpengerTilstand,
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

    object KlarTilBehandling : RettTilDagpengerTilstand {
        override val type: Tilstand.Type = KLAR_TIL_BEHANDLING
    }
}
