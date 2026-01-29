package no.nav.dagpenger.saksbehandling.statistikk

import no.nav.dagpenger.aktivitetslogg.aktivitet.Hendelse
import no.nav.dagpenger.saksbehandling.Configuration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class OppgaveTilstandsendring(
    val oppgaveId: UUID,
    val mottatt: LocalDate,
    val sakId: UUID,
    val behandlingId: UUID,
    val personIdent: String,
    val saksbehandlerIdent: String?,
    val beslutterIdent: String?,
    val versjon: String = Configuration.versjon,
    val tilstandsendring: StatistikkOppgaveTilstandsendring,
    val utløstAv: String,
) {
    data class StatistikkOppgaveTilstandsendring(
        val id: UUID,
        val tilstand: String,
        val tidspunkt: LocalDateTime,
    )
    override fun toString(): String =
        "StatistikkOppgave(oppgaveId=$oppgaveId, " +
                "mottatt=$mottatt," +
                "sakId=$sakId," +
                "behandlingId=$behandlingId," +
                "saksbehandlerIdent=$saksbehandlerIdent," +
                "beslutterIdent=$beslutterIdent," +
                "utløstAv=$utløstAv," +
                "tilstandsEndring=$tilstandsendring," +
                "versjon='$versjon')"

    fun asMap(): Map<String, Any> =
        buildMap {
            put("oppgaveId", oppgaveId.toString())
            put("mottatt", mottatt.toString())
            put("sakId", sakId.toString())
            put("behandlingId", behandlingId.toString())
            put("personIdent", personIdent)
            saksbehandlerIdent?.let { put("saksbehandlerIdent", it) }
            beslutterIdent?.let { put("beslutterIdent", it) }
            put("tilstandsendring", tilstandsendring)
            put("utløstAv", utløstAv)
            put("versjon", versjon)
        }

}

