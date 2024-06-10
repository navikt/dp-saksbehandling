package no.nav.dagpenger.saksbehandling

import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.utsending.hendelser.ArkiverbartBrevHendelse
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import java.util.UUID

class BehovLøsningMediatorTest {
    private val testRapid = TestRapid()
    private val oppgaveUUID: UUID = UUIDv7.ny()

    @Test
    fun `River filter`() {
        val mediator = mockk<UtsendingMediator>(relaxed = true)
        BehovLøsningMediator(
            utsendingMediator = mediator,
            rapidsConnection = testRapid,
        )

        //language=JSON
        testRapid.sendTestMessage(arkiverbartDokumentBehovLosning(oppgaveUUID))

        verify(exactly = 1) {
            mediator.mottaUrnTilArkiverbartFormatAvBrev(
                ArkiverbartBrevHendelse(
                    oppgaveId = oppgaveUUID,
                    pdfUrn = "urn:saksbehandling:$oppgaveUUID".toUrn(),
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
}
