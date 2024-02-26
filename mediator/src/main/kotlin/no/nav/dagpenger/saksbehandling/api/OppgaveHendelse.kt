package no.nav.dagpenger.saksbehandling.api

import java.util.UUID

data class OppgaveHendelse(val oppgaveId: UUID, val saksbehandlerSignatur: String)
