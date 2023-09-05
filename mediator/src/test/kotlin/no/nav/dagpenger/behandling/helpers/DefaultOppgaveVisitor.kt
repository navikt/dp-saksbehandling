package no.nav.dagpenger.behandling.helpers

import no.nav.dagpenger.behandling.Behandling
import no.nav.dagpenger.behandling.OppgaveVisitor
import no.nav.dagpenger.behandling.oppgave.Oppgave
import no.nav.dagpenger.behandling.oppgave.Saksbehandler
import java.time.LocalDateTime
import java.util.UUID

open class DefaultOppgaveVisitor(oppgave: Oppgave) : OppgaveVisitor {
    lateinit var oppgaveId: UUID
    lateinit var behandling: Behandling

    init {
        oppgave.accept(this)
    }

    override fun visit(oppgaveId: UUID, opprettet: LocalDateTime, utføresAv: Saksbehandler?) {
        this.oppgaveId = oppgaveId
    }

    override fun visit(behandling: Behandling) {
        this.behandling = behandling
    }
}
