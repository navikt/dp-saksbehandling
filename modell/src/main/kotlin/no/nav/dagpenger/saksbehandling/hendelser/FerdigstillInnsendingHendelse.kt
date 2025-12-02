package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.innsending.Aksjon
import java.util.UUID

data class FerdigstillInnsendingHendelse(
    val innsendingId: UUID,
    val aksjon: Aksjon,
    val vurdering: String,
    override val utførtAv: Saksbehandler,
) : Hendelse(utførtAv) {
    fun valgtSakId() = aksjon.valgtSakId
}
