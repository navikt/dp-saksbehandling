package no.nav.dagpenger.saksbehandling.utsending.hendelser

import no.nav.dagpenger.saksbehandling.UtsendingSak
import java.util.UUID

data class StartUtsendingHendelse(
    override val oppgaveId: UUID,
    val utsendingSak: UtsendingSak,
    val behandlingId: UUID,
    val ident: String,
    val brev: String? = null,
) : UtsendingHendelse
