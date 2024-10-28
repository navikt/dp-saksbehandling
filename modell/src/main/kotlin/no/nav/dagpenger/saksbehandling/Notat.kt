package no.nav.dagpenger.saksbehandling

import java.time.LocalDateTime
import java.util.UUID

class Notat(val notatId: UUID, tekst: String, val sistEndretTidspunkt: LocalDateTime) {
    private var tekst: String = tekst

    fun endreTekst(tekst: String) {
        this.tekst = tekst
    }
}
