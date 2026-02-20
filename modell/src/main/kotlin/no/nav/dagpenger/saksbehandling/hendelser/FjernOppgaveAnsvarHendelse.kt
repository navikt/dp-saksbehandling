package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Saksbehandler
import java.util.UUID

data class FjernOppgaveAnsvarHendelse(
    val oppgaveId: UUID,
    val årsak: String? = null,
    override val utførtAv: Saksbehandler,
) : Hendelse(utførtAv)
