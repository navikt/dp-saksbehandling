package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.innsending.Aksjon
import java.util.UUID

data class InnsendingFerdigstiltHendelse(
    val innsendingId: UUID,
    val aksjon: Aksjon,
    // TODO: Bør denne hete noe mer informativt enn behandlingId? F.eks. resultatBehandlingId?
    // Eller bør vi sende med hele InnsendingResultat her? Alternativt også aksjon som String,
    // siden det skal lagres i tilstandsloggen
    val behandlingId: UUID?,
    override val utførtAv: Saksbehandler,
) : Hendelse(utførtAv)
