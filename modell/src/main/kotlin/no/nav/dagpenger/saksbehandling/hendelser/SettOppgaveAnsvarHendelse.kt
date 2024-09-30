package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Aktør
import java.util.UUID

data class SettOppgaveAnsvarHendelse(
    val oppgaveId: UUID,
    override val ansvarligIdent: String,
    override val utførtAv: Aktør,
) : AnsvarHendelse(utførtAv, ansvarligIdent)
