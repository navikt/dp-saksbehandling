package no.nav.dagpenger.saksbehandling

import java.time.LocalDateTime
import java.util.UUID

class Notat(val notatId: UUID, tekst: String, val sistEndretTidspunkt: LocalDateTime, val skrevetAv: String) {
    private var tekst: String = tekst

    fun endreTekst(tekst: String) {
        this.tekst = tekst
    }

    fun hentTekst(): String {
        return tekst
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Notat) return false

        return (notatId == other.notatId) && (tekst == other.tekst)
    }

    override fun hashCode(): Int {
        var result = notatId.hashCode()
        result = 31 * result + tekst.hashCode()
        return result
    }
}
