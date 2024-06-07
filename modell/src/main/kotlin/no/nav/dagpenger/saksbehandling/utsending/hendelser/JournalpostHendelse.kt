package no.nav.dagpenger.saksbehandling.utsending.hendelser

import java.util.UUID

data class JournalpostHendelse(override val oppgaveId: UUID, val journalpostId: String) : UtsendingHendelse
