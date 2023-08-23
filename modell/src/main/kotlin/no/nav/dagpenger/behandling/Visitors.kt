package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.oppgave.Oppgave
import java.util.UUID

interface PersonVisitor {
    fun visit(saker: Set<Sak>) {}
}

interface BehandlingVisitor {
    fun visit(behandlingId: UUID, sak: Sak) {}
}

interface OppgaveVisitor : BehandlingVisitor {
    fun visit(oppgaveId: UUID) {}
    fun visit(behandling: Behandling) {}
}

open class DefaultOppgaveVisitor(oppgave: Oppgave) : OppgaveVisitor {
    lateinit var oppgaveId: UUID
    lateinit var behandling: Behandling

    init {
        oppgave.accept(this)
    }

    override fun visit(oppgaveId: UUID) {
        this.oppgaveId = oppgaveId
    }

    override fun visit(behandling: Behandling) {
        this.behandling = behandling
    }
}
