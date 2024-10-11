package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Saksbehandler
import java.util.UUID

data class ToTrinnskontrollHendelse(
    val oppgaveId: UUID,
    override val ansvarligIdent: String,
    override val utførtAv: Saksbehandler,
) : AnsvarHendelse(ansvarligIdent = ansvarligIdent, utførtAv = utførtAv)
