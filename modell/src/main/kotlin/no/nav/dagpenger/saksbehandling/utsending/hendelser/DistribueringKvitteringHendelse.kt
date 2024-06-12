package no.nav.dagpenger.saksbehandling.utsending.hendelser

import java.util.UUID

data class DistribueringKvitteringHendelse(
    override val oppgaveId: UUID,
    val distribusjonId: String,
    val journalpostId: String,
) : UtsendingHendelse
