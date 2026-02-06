package no.nav.dagpenger.saksbehandling.statistikk

import no.nav.dagpenger.saksbehandling.Configuration
import java.time.LocalDateTime
import java.util.UUID

data class OppgaveITilstand(
    val oppgaveId: UUID,
    val mottatt: LocalDateTime,
    val sakId: UUID,
    val behandlingId: UUID,
    val personIdent: String,
    val saksbehandlerIdent: String?,
    val beslutterIdent: String?,
    val versjon: String = Configuration.versjon,
    val tilstandsendring: Tilstandsendring,
    val utløstAv: String,
    val behandlingResultat: String?,
) {
    data class Tilstandsendring(
        val sekvensnummer: Long,
        val tilstandsendringId: UUID,
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
            "tilstandsendring=$tilstandsendring," +
            "versjon='$versjon'," +
            "behandlingResultat=$behandlingResultat)"

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
            behandlingResultat?.let { put("behandlingResultat", it) }
        }
}
