package no.nav.dagpenger.saksbehandling.api

import java.util.UUID

data class GodkjennBehandlingHendelse(val behandlingId: UUID, val saksbehandlerSignatur: String)
