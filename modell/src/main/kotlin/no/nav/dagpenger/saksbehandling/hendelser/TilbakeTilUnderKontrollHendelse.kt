package no.nav.dagpenger.saksbehandling.hendelser

import java.util.UUID

data class TilbakeTilUnderKontrollHendelse(
    val oppgaveId: UUID,
    override val utførtAv: String,
    override val ansvarligIdent: String,
) :
    AnsvarHendelse(
            utførtAv = utførtAv,
            ansvarligIdent = ansvarligIdent,
        )
