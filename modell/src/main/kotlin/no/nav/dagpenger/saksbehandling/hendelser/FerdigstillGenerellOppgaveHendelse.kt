package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.generell.GenerellOppgaveAksjon
import java.util.UUID

data class FerdigstillGenerellOppgaveHendelse(
    val generellOppgaveId: UUID,
    val aksjon: GenerellOppgaveAksjon,
    val vurdering: String?,
    override val utførtAv: Saksbehandler,
) : Hendelse(utførtAv)
