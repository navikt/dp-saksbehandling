package no.nav.dagpenger.saksbehandling

import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.helper.arkiverbartDokumentBehovLøsning
import no.nav.dagpenger.saksbehandling.mottak.BehovLøsningMottak
import no.nav.dagpenger.saksbehandling.utsending.hendelser.ArkiverbartBrevHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.DistribuertHendelse
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

        val pdfUrn = "urn:pdf:1234".toUrn()
        testRapid.sendTestMessage(
            arkiverbartDokumentBehovLøsning(
                oppgaveUUID = oppgaveUUID,
                pdfUrnString = pdfUrn.toString(),
            ),
        )

        verify(exactly = 1) {
            mediator.mottaUrnTilArkiverbartFormatAvBrev(
                ArkiverbartBrevHendelse(
                    oppgaveId = oppgaveUUID,
                    pdfUrn = pdfUrn,
                ),
            )
        }

        val journalPostId = "jp1"
        val distribusjonId = "distribusjonId"
        testRapid.sendTestMessage(
            distribuertDokumentBehovLøsning(
                oppgaveUUID = oppgaveUUID,
                journalpostId = journalPostId,
                distribueringId = distribusjonId,
            ),
        )
        verify(exactly = 1) {
            mediator.mottaDistribuertKvittering(
                DistribuertHendelse(
                    oppgaveId = oppgaveUUID,
                    distribusjonId = distribusjonId,
                    journalpostId = journalPostId,
                ),
            )
        }
    }

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
