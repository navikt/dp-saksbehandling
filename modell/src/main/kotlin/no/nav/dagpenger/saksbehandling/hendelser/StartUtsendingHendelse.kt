package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.utsending.hendelser.UtsendingHendelse
import java.util.UUID

data class StartUtsendingHendelse(
    override val oppgaveId: UUID,
    val sak: Sak,
    val behandlingId: UUID,
    val ident: String,
) : UtsendingHendelse
