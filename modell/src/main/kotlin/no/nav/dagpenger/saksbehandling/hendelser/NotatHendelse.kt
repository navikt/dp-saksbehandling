package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Saksbehandler
import java.util.UUID

data class TomtNotatHendelse(
    val oppgaveId: UUID,
    override val utførtAv: Saksbehandler,
) : Hendelse(utførtAv)

data class NotatHendelse(
    val oppgaveId: UUID,
    val tekst: String,
    override val utførtAv: Saksbehandler,
) : Hendelse(utførtAv) {
    init {
        require(tekst.isNotBlank()) { "Notat kan ikke være tomt" }
    }
}
