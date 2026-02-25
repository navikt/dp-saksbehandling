package no.nav.dagpenger.saksbehandling.helper

import no.nav.dagpenger.saksbehandling.utsending.ArkiverbartBrevBehov
import no.nav.dagpenger.saksbehandling.utsending.DistribueringBehov
import no.nav.dagpenger.saksbehandling.utsending.JournalføringBehov
import no.nav.dagpenger.saksbehandling.utsending.UtsendingType
import java.util.UUID

internal fun behandlingsresultatEvent(
    ident: String,
    behandlingId: String,
    behandletHendelseId: String,
    behandletHendelseType: String = "Søknad",
    harRett: Boolean = true,
    basertPå: UUID? = null,
    eventNavn: String = "behandlingsresultat",
    automatiskBehandling: Boolean = false,
): String {
    //language=JSON
    return """
        {
          "@event_name": "$eventNavn",
          "ident": "$ident",
          "behandlingId": "$behandlingId",
          "behandletHendelse": {
            "id": "$behandletHendelseId",
            "type": "$behandletHendelseType"
          },
          "automatisk": $automatiskBehandling,
            ${basertPå?.let { """"basertPå": "$it",""" } ?: ""}
          "rettighetsperioder": [
            {
              "fraOgMed": "2025-09-09",
              "harRett": $harRett
            }
          ]
        }
        """.trimIndent()
}

internal fun distribuertDokumentBehovLøsning(
    behandlingId: UUID,
    journalpostId: String,
    distribusjonId: String,
    ident: String = "12345678901",
    utsendingId: UUID = UUID.randomUUID(),
    utsendingType: UtsendingType = UtsendingType.VEDTAK_DAGPENGER,
): String {
    //language=JSON
    return """
        {
          "@event_name": "behov",
          "behandlingId": "$behandlingId",
          "journalpostId": "$journalpostId",
          "ident": "$ident",
          "utsendingId": "$utsendingId",
          "utsendingType": "${utsendingType.name}",
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
