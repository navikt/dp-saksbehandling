package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Emneknagg
import no.nav.dagpenger.saksbehandling.Emneknagg.AvbrytBehandling.AVBRUTT_ANNET
import no.nav.dagpenger.saksbehandling.Saksbehandler
import java.util.UUID

class AvbrytOppgaveHendelse(
    val oppgaveId: UUID,
    val navIdent: String,
    val årsak: Emneknagg.AvbrytBehandling = AVBRUTT_ANNET,
    override val utførtAv: Saksbehandler,
) : Hendelse(utførtAv)
