package no.nav.dagpenger.saksbehandling.api

import java.util.UUID

data class OppdaterOppgaveHendelse(val oppgaveId: UUID, val saksbehandlerSignatur: String)
