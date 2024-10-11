package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Saksbehandler
import java.util.UUID

data class TilbakeTilUnderKontrollHendelse(
    val oppgaveId: UUID,
    override val utførtAv: Saksbehandler,
    override val ansvarligIdent: String,
) :
    AnsvarHendelse(
            utførtAv = utførtAv,
            ansvarligIdent = ansvarligIdent,
        )
