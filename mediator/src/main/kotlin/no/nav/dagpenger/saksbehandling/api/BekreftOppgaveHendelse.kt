package no.nav.dagpenger.saksbehandling.api

import java.util.UUID

data class BekreftOppgaveHendelse(val oppgaveId: UUID, val saksbehandlerSignatur: String)
