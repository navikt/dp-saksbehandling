package no.nav.dagpenger.saksbehandling.api

import java.util.UUID

data class AvbrytBehandlingHendelse(val oppgaveId: UUID, val saksbehandlerSignatur: String)
