package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Aktør
import java.util.UUID

data class FjernOppgaveAnsvarHendelse(
    val oppgaveId: UUID,
    override val utførtAv: Aktør,
) : Hendelse(utførtAv)
