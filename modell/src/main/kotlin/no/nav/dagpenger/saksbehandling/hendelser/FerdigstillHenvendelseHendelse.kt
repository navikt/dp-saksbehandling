package no.nav.dagpenger.saksbehandling.hendelser

import java.util.UUID
import no.nav.dagpenger.saksbehandling.Behandler

data class FerdigstillHenvendelseHendelse(
    val henvendelseId: UUID,
    val aksjon: Aksjon,
    override val utførtAv: Behandler,
) : Hendelse(utførtAv)

data class HenvendelseFerdigstiltHendelse(
    val ferdigstillHenvendelseHendelse: FerdigstillHenvendelseHendelse,
    val behandlingId: UUID,
    override val utførtAv: Behandler,
) : Hendelse(utførtAv)

sealed class Aksjon {
    object Avslutt : Aksjon()
    object OpprettManuellBehandling : Aksjon()
    object OpprettKlage : Aksjon()
}