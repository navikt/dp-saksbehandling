package no.nav.dagpenger.saksbehandling.utsending.hendelser

import java.util.UUID

data class UtsendingKvitteringHendelse(val utsendingId: UUID, val journalpostId: String)
