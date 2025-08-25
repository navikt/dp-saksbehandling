package no.nav.dagpenger.saksbehandling.hendelser

import java.util.UUID
import no.nav.dagpenger.saksbehandling.Saksbehandler

data class EndreMeldingOmVedtakKildeHendelse(
    val oppgaveId: UUID,
    val meldingOmVedtakKilde: MeldingOmVedtakKilde,
    override val utførtAv: Saksbehandler,
) : Hendelse(utførtAv) {
    init {
        require(meldingOmVedtakKilde.isNotBlank()) { "Notat kan ikke være tomt" }
    }
}