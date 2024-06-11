package no.nav.dagpenger.saksbehandling

import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.utsending.hendelser.ArkiverbartBrevHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.DistribueringKvitteringHendelse
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import java.util.UUID

class BehovLøsningMottakTest {
    private val testRapid = TestRapid()
    private val oppgaveUUID: UUID = UUIDv7.ny()

    @Test
    fun `River filter`() {
        val mediator = mockk<UtsendingMediator>(relaxed = true)
        BehovLøsningMottak(
            utsendingMediator = mediator,
            rapidsConnection = testRapid,
        )

        testRapid.sendTestMessage(arkiverbartDokumentBehovLosning(oppgaveUUID))

        verify(exactly = 1) {
            mediator.mottaUrnTilArkiverbartFormatAvBrev(
                ArkiverbartBrevHendelse(
                    oppgaveId = oppgaveUUID,
                    pdfUrn = "urn:saksbehandling:$oppgaveUUID".toUrn(),
                ),
            )
        }

        val journalPostId = "jp1"
        // todo

        val distribusjonId = "distribusjonId"
        testRapid.sendTestMessage(
            distribuertDokumentBehovLøsning(
                oppgaveUUID = oppgaveUUID,
                journalpostId = journalPostId,
                distribueringId = distribusjonId,
            ),
        )
        verify(exactly = 1) {
            mediator.mottaDistribueringKvittering(
                DistribueringKvitteringHendelse(
                    oppgaveId = oppgaveUUID,
                    distribueringId = distribusjonId,
                    journalpostId = journalPostId,
                ),
            )
        }
    }

    //language=JSON
    private fun arkiverbartDokumentBehovLosning(oppgaveUUID: UUID) =
        """
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
              "urn": "urn:saksbehandling:$oppgaveUUID"
            }
          }
        }
        """.trimIndent()

    private fun distribuertDokumentBehovLøsning(
        oppgaveUUID: UUID,
        journalpostId: String,
        distribueringId: String,
    ): String {
        //language=JSON
        return """
            {
              "@event_name": "behov",
              "oppgaveId": "$oppgaveUUID",
              "journalpostId": "$journalpostId",
              "@behov": [
                "DistribueringBehov"
              ],
              "@løsning": {
                "DistribueringBehov": {
                  "distribueringId": "$distribueringId"
                }
              }
            }
            """.trimIndent()
    }
}
