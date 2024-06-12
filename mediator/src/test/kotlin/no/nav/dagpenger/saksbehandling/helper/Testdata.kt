package no.nav.dagpenger.saksbehandling.helper

import no.nav.dagpenger.saksbehandling.db.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.db.lagBehandling
import no.nav.dagpenger.saksbehandling.db.lagOppgave
import no.nav.dagpenger.saksbehandling.utsending.DistribueringBehov
import no.nav.dagpenger.saksbehandling.utsending.JournalføringBehov
import java.util.UUID
import javax.sql.DataSource

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
            "ArkiverbartDokument": {
              "metainfo": {
                "filnavn": "netto.pdf",
                "filtype": "PDF"
              },
              "urn": "$pdfUrnString"
            }
          }
        }
        """.trimIndent()
}

internal fun lagreOppgaveOgBehandling(dataSource: DataSource): Pair<UUID, UUID> {
    val behandling = lagBehandling()
    val oppgave = lagOppgave()
    val repository = PostgresOppgaveRepository(dataSource)
    repository.lagre(oppgave)
    return Pair(oppgave.oppgaveId, behandling.behandlingId)
}
