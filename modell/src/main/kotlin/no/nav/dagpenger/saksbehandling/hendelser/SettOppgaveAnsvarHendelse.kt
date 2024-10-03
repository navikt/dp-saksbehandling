package no.nav.dagpenger.saksbehandling.hendelser

import java.util.UUID

data class SettOppgaveAnsvarHendelse(
    val oppgaveId: UUID,
    override val ansvarligIdent: String,
    override val utførtAv: String,
) : AnsvarHendelse(utførtAv, ansvarligIdent)
