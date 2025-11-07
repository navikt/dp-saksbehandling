package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Behandler
import java.util.UUID

data class InnsendingFerdigstiltHendelse(
    val innsendingId: UUID,
    val aksjon: String,
    val behandlingId: UUID?,
    override val utførtAv: Behandler,
) : Hendelse(utførtAv)
