package no.nav.dagpenger.saksbehandling.statistikk

import no.nav.dagpenger.saksbehandling.Configuration
import no.nav.dagpenger.saksbehandling.Emneknagg
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
    val emneknagger: Set<String> = emptySet(),
    val behandlingResultat: String?,
    val behandlingÅrsak: String?,
    val fagsystem: String?,
    val arenaSakId: String?,
    val resultatBegrunnelse: String?,
    val relatertBehandlingId: UUID?,
) {
    data class Tilstandsendring(
        val sekvensnummer: Long,
        val tilstandsendringId: UUID,
        val tilstand: String,
        val tidspunkt: LocalDateTime,
    )

    override fun toString(): String =
        "StatistikkOppgave(oppgaveId=$oppgaveId, " +
            "mottatt=$mottatt, " +
            "sakId=$sakId, " +
            "behandlingId=$behandlingId, " +
            "saksbehandlerIdent=$saksbehandlerIdent, " +
            "beslutterIdent=$beslutterIdent, " +
            "utløstAv=$utløstAv, " +
            "emneknagger=$emneknagger, " +
            "tilstandsendring=$tilstandsendring, " +
            "versjon='$versjon', " +
            "behandlingResultat=$behandlingResultat, " +
            "behandlingÅrsak=$behandlingÅrsak, " +
            "fagsystem=$fagsystem, " +
            "arenaSakId=$arenaSakId, " +
            "resultatBegrunnelse=$resultatBegrunnelse, " +
            "relatertBehandlingId=$relatertBehandlingId)"

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
            sakYtelse()?.let { put("sakYtelse", it) }
            put("versjon", versjon)
            behandlingResultat?.let { put("behandlingResultat", it) }
            behandlingÅrsak?.let { put("behandlingÅrsak", it) }
            fagsystem?.let { put("fagsystem", it) }
            arenaSakId?.let { put("arenaSakId", it) }
            resultatBegrunnelse?.let { put("resultatBegrunnelse", it) }
            relatertBehandlingId?.let { put("relatertBehandlingId", it.toString()) }
        }

    fun sakYtelse(): String? =
        emneknagger
            .map { emneknagg ->
                when (emneknagg) {
                    Emneknagg.Regelknagg.RETTIGHET_ORDINÆR.visningsnavn -> "ORDINÆR"
                    Emneknagg.Regelknagg.RETTIGHET_PERMITTERT.visningsnavn -> "PERMITTERT"
                    Emneknagg.Regelknagg.RETTIGHET_PERMITTERT_FISK.visningsnavn -> "PERMITTERTFISK"
                    Emneknagg.Regelknagg.RETTIGHET_KONKURS.visningsnavn -> "LØNNSGARANTI"
                    Emneknagg.Regelknagg.RETTIGHET_VERNEPLIKT.visningsnavn -> "VERNEPLIKT"
                    Emneknagg.Regelknagg.RETTIGHET_UTDANNING_OPPLÆRING.visningsnavn -> "UTDANNING"
                    Emneknagg.Regelknagg.RETTIGHET_EKSPORT.visningsnavn -> "EKSPORT"
                    Emneknagg.Regelknagg.RETTIGHET_ETABLERING.visningsnavn -> "ETABLERING"
                    else -> null
                }
            }.filterNotNull()
            .joinToString(separator = "_")
            .let { if (it.isBlank()) null else it }
}
