package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.oppfolging.OppfølgingAksjon
import java.util.UUID

data class FerdigstillOppfølgingHendelse(
    val oppfølgingId: UUID,
    val aksjon: OppfølgingAksjon,
    val vurdering: String?,
    override val utførtAv: Saksbehandler,
) : Hendelse(utførtAv)
