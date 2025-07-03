package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Saksbehandler
import java.util.UUID

data class GodkjentBehandlingHendelse(
    val oppgaveId: UUID,
    val meldingOmVedtak: String? = null,
    override val utførtAv: Saksbehandler,
) : Hendelse(utførtAv)
