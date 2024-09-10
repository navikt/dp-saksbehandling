package no.nav.dagpenger.saksbehandling.helper

import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.db.lagOppgave
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.utsending.ArkiverbartBrevBehov
import no.nav.dagpenger.saksbehandling.utsending.DistribueringBehov
import no.nav.dagpenger.saksbehandling.utsending.JournalføringBehov
import java.util.UUID
import javax.sql.DataSource

internal fun vedtakFattetHendelse(
    ident: String,
    søknadId: UUID,
    behandlingId: UUID,
    sakId: Int,
): String {
    //language=JSON
    return """{
      "@event_name": "vedtak_fattet",
      "søknadId": "$søknadId",
      "behandlingId": "$behandlingId",
      "ident": "$ident",
      "automatisk": true,
      "fagsakId": $sakId
    }"""
}

internal fun vedtakFattetHendelseMedMeldingOmVedtakProdusent(
    ident: String,
    søknadId: UUID,
    behandlingId: UUID,
    sakId: Int,
): String {
    //language=JSON
    return """{
      "@event_name": "vedtak_fattet",
      "søknadId": "$søknadId",
      "meldingOmVedtakProdusent": "Arena",
      "behandlingId": "$behandlingId",
      "ident": "$ident",
      "fagsakId": $sakId,
      "automatisk": true
    }"""
}

internal fun distribuertDokumentBehovLøsning(
    oppgaveId: UUID,
    journalpostId: String,
    distribusjonId: String,
): String {
    //language=JSON
    return """
        {
          "@event_name": "behov",
          "oppgaveId": "$oppgaveId",
          "journalpostId": "$journalpostId",
          "@behov": [
            "${DistribueringBehov.BEHOV_NAVN}"
          ],
          "@løsning": {
            "${DistribueringBehov.BEHOV_NAVN}": {
              "distribueringId": "$distribusjonId"
            }
          }
        }
        """.trimIndent()
}

internal fun journalføringBehovLøsning(
    oppgaveId: UUID,
    journalpostId: String,
): String {
//language=JSON
    return """
           {
          "@event_name": "behov",
          "oppgaveId": "$oppgaveId",
          "@behov": ["${JournalføringBehov.BEHOV_NAVN}"],
          "@løsning": {
            "${JournalføringBehov.BEHOV_NAVN}": {
                "journalpostId": "$journalpostId"
            }
          }
        }
        """.trimIndent()
}

internal fun arkiverbartDokumentBehovLøsning(
    oppgaveUUID: UUID,
    pdfUrnString: String,
    final: Boolean? = null,
): String {
//language=JSON
    return """
        {
          "@event_name": "behov",
          "oppgaveId": "$oppgaveUUID",
          "@behov": ["${ArkiverbartBrevBehov.BEHOV_NAVN}"],
          ${final?.let { """"@final": $it,""" } ?: ""}
          "@løsning": {
            "${ArkiverbartBrevBehov.BEHOV_NAVN}": {
              "metainfo": {
                "dokumentNavn": "netto.pdf",
                "dokumentType": "PDF"
              },
              "urn": "$pdfUrnString"
            }
          }
        }
        """.trimIndent()
}

internal fun lagreOppgave(dataSource: DataSource): Oppgave {
    val oppgave = lagOppgave()
    val repository = PostgresOppgaveRepository(dataSource)
    repository.lagre(oppgave)
    return oppgave
}
