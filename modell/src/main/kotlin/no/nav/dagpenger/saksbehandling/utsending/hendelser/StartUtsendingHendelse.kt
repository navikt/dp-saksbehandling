package no.nav.dagpenger.saksbehandling.utsending.hendelser

import no.nav.dagpenger.saksbehandling.Sak
import java.util.UUID

data class StartUtsendingHendelse(
    override val oppgaveId: UUID,
    val sak: Sak,
    val behandlingId: UUID,
    val ident: String,
) : UtsendingHendelse
