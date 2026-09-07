package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Emneknagg
import no.nav.dagpenger.saksbehandling.Saksbehandler
import java.util.UUID

class AvbrytKlageHendelse(
    val oppgaveId: UUID,
    val navIdent: String,
    val årsak: Emneknagg.AvbrytKlage = Emneknagg.AvbrytKlage.AVBRUTT_ANNET,
    override val utførtAv: Saksbehandler,
) : Hendelse(utførtAv)
