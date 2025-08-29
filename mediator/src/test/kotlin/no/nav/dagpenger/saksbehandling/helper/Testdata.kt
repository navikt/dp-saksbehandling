package no.nav.dagpenger.saksbehandling.helper

import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.lagOppgave
import no.nav.dagpenger.saksbehandling.lagPerson
import no.nav.dagpenger.saksbehandling.lagTilfeldigIdent
import no.nav.dagpenger.saksbehandling.utsending.ArkiverbartBrevBehov
import no.nav.dagpenger.saksbehandling.utsending.DistribueringBehov
import no.nav.dagpenger.saksbehandling.utsending.JournalføringBehov
import java.util.UUID
import javax.sql.DataSource

internal fun vedtakFattetHendelse(
    ident: String,
    behandletHendelseId: String,
    behandletHendelseType: String = "Søknad",
    behandlingId: UUID,
    utfall: Boolean = true,
    sakId: Int = 123,
    automatiskBehandlet: Boolean = false,
): String {
    //language=JSON
    return """{
      "@event_name": "vedtak_fattet",
      "behandletHendelse": {
        "datatype": "UUID",
        "id": "$behandletHendelseId",
        "type": "$behandletHendelseType"
      },
       "fastsatt": {
         "utfall": $utfall
       },
      "behandlingId": "$behandlingId",
      "ident": "$ident",
      "fagsakId": $sakId,
      "automatisk": $automatiskBehandlet
    }"""
}

internal fun distribuertDokumentBehovLøsning(
    behandlingId: UUID,
    journalpostId: String,
    distribusjonId: String,
): String {
    //language=JSON
    return """
        {
          "@event_name": "behov",
          "behandlingId": "$behandlingId",
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
    behandlingId: UUID,
    journalpostId: String,
): String {
//language=JSON
    return """
           {
          "@event_name": "behov",
          "behandlingId": "$behandlingId",
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
    behandlingId: UUID,
    pdfUrnString: String,
    final: Boolean? = null,
): String {
//language=JSON
    return """
        {
          "@event_name": "behov",
          "behandlingId": "$behandlingId",
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

internal fun lagreOppgave(
    dataSource: DataSource,
    behandlingId: UUID = UUIDv7.ny(),
    personIdent: String = lagTilfeldigIdent(),
): Oppgave {
    val oppgave =
        lagOppgave(
            behandlingId = behandlingId,
            person = lagPerson(ident = personIdent),
        )

    val repository = PostgresOppgaveRepository(dataSource)
    repository.lagre(oppgave)
    return oppgave
}
