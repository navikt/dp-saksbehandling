package no.nav.dagpenger.saksbehandling

import io.kotest.assertions.throwables.shouldThrow
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.helper.arkiverbartDokumentBehovLøsning
import no.nav.dagpenger.saksbehandling.helper.distribuertDokumentBehovLøsning
import no.nav.dagpenger.saksbehandling.helper.journalføringBehovLøsning
import no.nav.dagpenger.saksbehandling.mottak.BehovLøsningMottak
import no.nav.dagpenger.saksbehandling.utsending.hendelser.ArkiverbartBrevHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.DistribuertHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.JournalførtHendelse
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import java.util.UUID

class BehovLøsningMottakTest {
    private val testRapid = TestRapid()
    private val oppgaveId: UUID = UUIDv7.ny()

    @Test
    fun `Tar imot de forventede løsningene på behovene`() {
        val mediator = mockk<UtsendingMediator>(relaxed = true)
        BehovLøsningMottak(
            utsendingMediator = mediator,
            rapidsConnection = testRapid,
        )

        val pdfUrn = "urn:pdf:1234".toUrn()
        testRapid.sendTestMessage(
            arkiverbartDokumentBehovLøsning(
                oppgaveUUID = oppgaveId,
                pdfUrnString = pdfUrn.toString(),
            ),
        )

        verify(exactly = 1) {
            mediator.mottaUrnTilArkiverbartFormatAvBrev(
                ArkiverbartBrevHendelse(
                    oppgaveId = oppgaveId,
                    pdfUrn = pdfUrn,
                ),
            )
        }

        val journalpostId = "jp1"
        testRapid.sendTestMessage(
            journalføringBehovLøsning(
                oppgaveId = oppgaveId,
                journalpostId = journalpostId,
            ),
        )
        verify(exactly = 1) {
            mediator.mottaJournalførtKvittering(
                JournalførtHendelse(
                    oppgaveId = oppgaveId,
                    journalpostId = journalpostId,
                ),
            )
        }

        val distribusjonId = "distribusjonId"
        testRapid.sendTestMessage(
            distribuertDokumentBehovLøsning(
                oppgaveId = oppgaveId,
                journalpostId = journalpostId,
                distribusjonId = distribusjonId,
            ),
        )
        verify(exactly = 1) {
            mediator.mottaDistribuertKvittering(
                DistribuertHendelse(
                    oppgaveId = oppgaveId,
                    distribusjonId = distribusjonId,
                    journalpostId = journalpostId,
                ),
            )
        }
    }

    @Test
    fun `Skal ignorere pakker med @final`() {
        val mediator =
            mockk<UtsendingMediator>(relaxed = true)
        BehovLøsningMottak(
            utsendingMediator = mediator,
            rapidsConnection = testRapid,
        )

        val message =
            arkiverbartDokumentBehovLøsning(
                oppgaveUUID = oppgaveId,
                pdfUrnString = "urn:pdf:1234",
                final = true,
            )
        testRapid.sendTestMessage(
            message,
        )
        verify(exactly = 0) {
            mediator.mottaUrnTilArkiverbartFormatAvBrev(any())
        }
    }

    @Test
    fun `Skal kaste feil hvis vi ikke kan håndtere forventet løsning`() {
        val mediator =
            mockk<UtsendingMediator>(relaxed = true).also {
                every { it.mottaUrnTilArkiverbartFormatAvBrev(any()) } throws RuntimeException()
            }
        BehovLøsningMottak(
            utsendingMediator = mediator,
            rapidsConnection = testRapid,
        )

        shouldThrow<RuntimeException> {
            testRapid.sendTestMessage(
                arkiverbartDokumentBehovLøsning(
                    oppgaveUUID = oppgaveId,
                    pdfUrnString = "urn:pdf:1234",
                ),
            )
        }
    }
}
