package no.nav.dagpenger.saksbehandling.hendelser

import java.util.UUID

data class ToTrinnskontrollHendelse(
    val oppgaveId: UUID,
    override val ansvarligIdent: String,
    override val utførtAv: String,
) : AnsvarHendelse(ansvarligIdent = ansvarligIdent, utførtAv = utførtAv)
