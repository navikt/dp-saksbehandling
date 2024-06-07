package no.nav.dagpenger.saksbehandling.utsending.hendelser

import java.util.UUID

data class VedtaksbrevHendelse(override val oppgaveId: UUID, val brev: String) : UtsendingHendelse
