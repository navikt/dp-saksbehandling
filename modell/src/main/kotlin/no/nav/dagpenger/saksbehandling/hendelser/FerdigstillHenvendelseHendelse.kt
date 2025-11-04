package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Behandler
import java.util.UUID

data class FerdigstillHenvendelseHendelse(
    val henvendelseId: UUID,
    val aksjon: Aksjon,
    override val utførtAv: Behandler,
) : Hendelse(utførtAv)

data class HenvendelseFerdigstiltHendelse(
    val henvendelseId: UUID,
    val aksjon: String,
    val behandlingId: UUID?,
    override val utførtAv: Behandler,
) : Hendelse(utførtAv)

sealed class Aksjon {
    object Avslutt : Aksjon()

    data class OpprettManuellBehandling(val saksbehandlerToken: String) : Aksjon()

    data class OpprettKlage(val sakId: UUID) : Aksjon()
}
