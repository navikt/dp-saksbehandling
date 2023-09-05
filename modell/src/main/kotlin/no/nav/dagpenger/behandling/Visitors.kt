package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.oppgave.Saksbehandler
import java.time.LocalDateTime
import java.util.UUID

interface PersonVisitor {
    fun visit(saker: Set<Sak>) {}
}

interface BehandlingVisitor {
    fun visit(behandlingId: UUID, sak: Sak) {}
}

interface OppgaveVisitor : BehandlingVisitor {
    fun visit(oppgaveId: UUID, opprettet: LocalDateTime, utføresAv: Saksbehandler?) {}
    fun visit(behandling: Behandling) {}
}
