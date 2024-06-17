package no.nav.dagpenger.saksbehandling.helper

import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.db.lagOppgave
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.utsending.DistribueringBehov
import no.nav.dagpenger.saksbehandling.utsending.JournalføringBehov
import java.util.UUID
import javax.sql.DataSource

internal fun vedtakFattetHendelse(
    ident: String,
    søknadId: UUID,
    behandlingId: UUID,
    sakId: String,
): String {
    //language=JSON
    return """{
      "@event_name": "vedtak_fattet",
      "søknadId": "$søknadId",
      "behandlingId": "$behandlingId",
      "ident": "$ident",
      "sakId": {
        "id": "$sakId",
        "kontekst": "Arena"
      }
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
            "DistribueringBehov": {
              "distribueringId": "$distribusjonId"
            }
          }
        }
        """.trimIndent()
}

internal fun journalføringBehovLøsning(
    oppgaveUUID: UUID,
    journalpostId: String,
): String {
//language=JSON
    return """
           {
          "@event_name": "behov",
          "oppgaveId": "$oppgaveUUID",
          "@behov": ["${JournalføringBehov.BEHOV_NAVN}"],
          "@løsning": {
            "JournalføringBehov": {
                "journalpostId": "$journalpostId"
            }
          }
        }
        """.trimIndent()
}

internal fun arkiverbartDokumentBehovLøsning(
    oppgaveUUID: UUID,
    pdfUrnString: String,
): String {
//language=JSON
    return """
        {
          "@event_name": "behov",
          "oppgaveId": "$oppgaveUUID",
          "@behov": ["ArkiverbartDokumentBehov"],
          "@løsning": {
            "ArkiverbartDokumentBehov": {
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
