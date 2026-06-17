package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Saksbehandler
import java.util.UUID

class HuskelappHendelse(
    val oppgaveId: UUID,
    val tekst: String,
    override val utførtAv: Saksbehandler,
) : Hendelse(utførtAv) {
    init {
        require(tekst.isNotBlank()) { "Huskelapp kan ikke ha tom tekst" }
    }
}
