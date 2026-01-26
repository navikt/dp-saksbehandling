package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Emneknagg
import no.nav.dagpenger.saksbehandling.Saksbehandler
import java.util.UUID

data class AvbrytOppgaveHendelse(
    val oppgaveId: UUID,
    val navIdent: String,
    val årsak: Emneknagg = Emneknagg.AVBRUTT_ANNET,
    override val utførtAv: Saksbehandler,
) : Hendelse(utførtAv)
