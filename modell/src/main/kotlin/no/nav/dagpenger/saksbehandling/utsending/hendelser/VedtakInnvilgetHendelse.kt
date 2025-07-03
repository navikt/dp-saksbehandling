package no.nav.dagpenger.saksbehandling.utsending.hendelser

import java.util.UUID

data class VedtakInnvilgetHendelse(
    override val oppgaveId: UUID,
    val ident: String,
    val behandlingId: UUID,
) : UtsendingHendelse
