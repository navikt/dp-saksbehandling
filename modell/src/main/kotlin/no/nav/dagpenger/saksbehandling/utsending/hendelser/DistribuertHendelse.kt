package no.nav.dagpenger.saksbehandling.utsending.hendelser

import java.util.UUID

data class DistribuertHendelse(
    override val behandlingId: UUID,
    val distribusjonId: String,
    val journalpostId: String,
) : UtsendingHendelse
