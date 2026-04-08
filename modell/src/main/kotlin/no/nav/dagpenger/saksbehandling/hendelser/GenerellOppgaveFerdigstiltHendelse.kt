package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.generell.GenerellOppgaveAksjon
import java.util.UUID

data class GenerellOppgaveFerdigstiltHendelse(
    val generellOppgaveId: UUID,
    val aksjonType: GenerellOppgaveAksjon.Type,
    val opprettetBehandlingId: UUID?,
    override val utførtAv: Saksbehandler,
) : Hendelse(utførtAv)
