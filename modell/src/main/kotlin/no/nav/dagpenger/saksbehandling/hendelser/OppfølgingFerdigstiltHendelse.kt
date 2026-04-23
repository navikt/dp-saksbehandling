package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.oppfolging.OppfølgingAksjon
import java.util.UUID

data class OppfølgingFerdigstiltHendelse(
    val oppfølgingId: UUID,
    val aksjonType: OppfølgingAksjon.Type,
    val opprettetBehandlingId: UUID?,
    val opprettetOppgaveId: UUID? = null,
    override val utførtAv: Saksbehandler,
) : Hendelse(utførtAv)
