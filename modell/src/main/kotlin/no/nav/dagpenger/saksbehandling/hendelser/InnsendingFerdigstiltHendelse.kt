package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.innsending.Aksjon
import java.util.UUID

data class InnsendingFerdigstiltHendelse(
    val innsendingId: UUID,
    val aksjonType: Aksjon.Type,
    val opprettetBehandlingId: UUID?,
    override val utførtAv: Saksbehandler,
) : Hendelse(utførtAv)
