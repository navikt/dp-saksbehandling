package no.nav.dagpenger.saksbehandling.api

import java.util.UUID

data class TildelOppgaveHendelse(val oppgaveId: UUID, val navIdent: String)
