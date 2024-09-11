package no.nav.dagpenger.saksbehandling.hendelser

import java.util.UUID

data class GodkjentBehandlingHendelse(val oppgaveId: UUID, val meldingOmVedtak: String)
