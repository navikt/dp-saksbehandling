package no.nav.dagpenger.saksbehandling

import java.time.LocalDateTime
import java.util.UUID

class Huskelapp(
    val oppgaveId: UUID,
    tekst: String,
    val sistEndretTidspunkt: LocalDateTime,
    val skrevetAv: String,
) {
    private var tekst: String = tekst

    fun endreTekst(tekst: String) {
        this.tekst = tekst
    }

    fun hentTekst(): String = tekst

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Huskelapp) return false

        return (oppgaveId == other.oppgaveId) && (tekst == other.tekst)
    }

    override fun hashCode(): Int {
        var result = oppgaveId.hashCode()
        result = 31 * result + tekst.hashCode()
        return result
    }
}
