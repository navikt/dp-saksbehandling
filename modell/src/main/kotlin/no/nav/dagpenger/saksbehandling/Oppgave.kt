package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.RettTilDagpenger.MeldingOmVedtak
import no.nav.dagpenger.saksbehandling.RettTilDagpenger.Tilstand
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

sealed class Oppgave {
    abstract val oppgaveId: UUID
    abstract val opprettet: LocalDateTime
    abstract var behandlerIdent: String?
    protected abstract val emneknagger: MutableSet<String>
    protected abstract var tilstand: Tilstand
    protected abstract var utsattTil: LocalDate?
    protected abstract val tilstandslogg: OppgaveTilstandslogg
    abstract val person: Person
    abstract val behandling: Behandling
    protected abstract var meldingOmVedtak: MeldingOmVedtak
}
