package no.nav.dagpenger.saksbehandling.utsending.hendelser

import java.util.UUID

data class JournalførtHendelse(
    override val behandlingId: UUID,
    val journalpostId: String,
) : UtsendingHendelse
