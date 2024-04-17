package no.nav.dagpenger.saksbehandling.hendelser

import java.util.UUID

data class TildelOppgaveHendelse(val oppgaveId: UUID, val navIdent: String)
