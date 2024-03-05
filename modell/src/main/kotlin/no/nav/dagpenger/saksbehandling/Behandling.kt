package no.nav.dagpenger.saksbehandling

import java.util.UUID

// @todo: Tydeliggjøre forskjellen på oppgave og behandling? Trenger vi behandling? Har behandling tilstand?
data class Behandling(
    val behandlingId: UUID,
    val oppgave: Oppgave,
)
