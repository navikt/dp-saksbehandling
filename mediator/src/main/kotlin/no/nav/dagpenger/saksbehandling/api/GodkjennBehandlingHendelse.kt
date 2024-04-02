package no.nav.dagpenger.saksbehandling.api

import java.util.UUID

data class GodkjennBehandlingHendelse(val oppgaveId: UUID, val saksbehandlerSignatur: String)
