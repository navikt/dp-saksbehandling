package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import java.util.UUID

// @todo: Tydeliggjøre forskjellen på oppgave og behandling? Trenger vi behandling? Har behandling tilstand?
data class Behandling(
    val behandlingId: UUID,
    val oppgave: Oppgave,
) {
    fun håndter(forslagTilVedtakHendelse: ForslagTilVedtakHendelse) {
        oppgave.håndter(forslagTilVedtakHendelse)
    }
}
