package no.nav.dagpenger.saksbehandling.hendelser

import java.util.UUID

data class GodkjentBehandlingHendelse(
    val oppgaveId: UUID,
    val meldingOmVedtak: String,
    val saksbehandlerToken: String,
    override val utførtAv: String,
) : Hendelse(utførtAv)
