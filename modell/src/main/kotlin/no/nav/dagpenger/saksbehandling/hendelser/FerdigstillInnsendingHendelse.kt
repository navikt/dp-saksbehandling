package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Behandler
import no.nav.dagpenger.saksbehandling.innsending.Aksjon
import java.util.UUID

data class FerdigstillInnsendingHendelse(
    val innsendingId: UUID,
    val aksjon: Aksjon,
    override val utførtAv: Behandler,
) : Hendelse(utførtAv)
