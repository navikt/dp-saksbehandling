package no.nav.dagpenger.saksbehandling.helpers

import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.OppgaveVisitor
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.oppgave.Oppgave
import java.time.LocalDateTime
import java.util.UUID

open class DefaultOppgaveVisitor(oppgave: Oppgave) : OppgaveVisitor {
    lateinit var oppgaveId: UUID
    lateinit var behandling: Behandling

    init {
        oppgave.accept(this)
    }

    override fun visit(
        oppgaveUUID: UUID,
        opprettet: LocalDateTime,
        utføresAv: Saksbehandler?,
        emneknagger: Set<String>,
    ) {
        this.oppgaveId = oppgaveUUID
    }

    override fun visit(behandling: Behandling) {
        this.behandling = behandling
    }
}
