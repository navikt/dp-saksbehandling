package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Aktør
import java.util.UUID

data class OppgaveAnsvarHendelse(val oppgaveId: UUID, val navIdent: String, val aktør: Aktør) : AnsvarHendelse(
    utførtAv = aktør,
    ansvarligIdent = navIdent,
)

// data class SettOppgaveAnsvarHendelse(val oppgaveId: UUID, val navIdent: String, val aktør: Aktør) : AnsvarHendelse(utførtAv = aktør, ansvarligIdent = navIdent)
