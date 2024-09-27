package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Aktør
import java.util.UUID

data class GodkjentBehandlingHendelse(
    val oppgaveId: UUID,
    val meldingOmVedtak: String,
    val saksbehandlerToken: String,
    private val aktør: Aktør,
) : Hendelse(aktør)
