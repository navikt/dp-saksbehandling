package no.nav.dagpenger.saksbehandling.utsending

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.assertions.json.shouldEqualSpecifiedJson
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.helper.arkiverbartDokumentBehovLøsning
import no.nav.dagpenger.saksbehandling.helper.distribuertDokumentBehovLøsning
import no.nav.dagpenger.saksbehandling.helper.journalføringBehovLøsning
import no.nav.dagpenger.saksbehandling.helper.lagreOppgave
import no.nav.dagpenger.saksbehandling.toUrn
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.AvventerArkiverbarVersjonAvBrev
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.AvventerDistribuering
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.AvventerJournalføring
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.Distribuert
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.VenterPåVedtak
import no.nav.dagpenger.saksbehandling.utsending.db.PostgresUtsendingRepository
import no.nav.dagpenger.saksbehandling.utsending.mottak.UtsendingBehovLøsningMottak
import no.nav.dagpenger.saksbehandling.utsending.mottak.UtsendingMottak
import org.junit.jupiter.api.Test
import java.util.Base64

class UtsendingMediatorTest {
    private val rapid = TestRapid()

    @Test
    fun `livssyklus for en utsending`() {
        withMigratedDb { datasource ->
            val oppgave = lagreOppgave(datasource)
            val oppgaveId = oppgave.oppgaveId
            val behandlingId = oppgave.behandlingId

            val utsendingRepository = PostgresUtsendingRepository(datasource)
            val utsendingMediator =
                UtsendingMediator(repository = utsendingRepository).also {
                    it.setRapidsConnection(rapid)
                }
            UtsendingMottak(
                rapidsConnection = rapid,
                utsendingMediator = utsendingMediator,
            )

            UtsendingBehovLøsningMottak(
                utsendingMediator = utsendingMediator,
                rapidsConnection = rapid,
            )

            val htmlBrev = "<H1>Hei</H1><p>Her er et brev</p>"
            utsendingMediator.opprettUtsending(oppgaveId, htmlBrev, oppgave.ident)

            var utsending = utsendingRepository.hent(oppgaveId)
            utsending.oppgaveId shouldBe oppgaveId
            utsending.tilstand().type shouldBe VenterPåVedtak
            utsending.brev() shouldBe htmlBrev

            val sak = Sak("sakId", "fagsystem")
            rapid.sendTestMessage(
                //language=JSON
                """
                {
                    "@event_name": "start_utsending",
                    "oppgaveId": "$oppgaveId",
                    "behandlingId": "$behandlingId",
                    "ident": "12345678901",
                    "sak": {
                        "id": "${sak.id}",
                        "kontekst": "${sak.kontekst}"
                    }
                }
                """,
            )

            utsending = utsendingRepository.hent(oppgaveId)
            utsending.sak() shouldBe sak
            utsending.tilstand().type shouldBe AvventerArkiverbarVersjonAvBrev

            rapid.inspektør.size shouldBe 1
            val htmlBrevAsBase64 = Base64.getEncoder().encode(htmlBrev.toByteArray()).toString(Charsets.UTF_8)
            rapid.inspektør.message(0).toString() shouldEqualSpecifiedJson
                //language=JSON
                """
                {
                   "@event_name": "behov",
                   "@behov": [
                     "${ArkiverbartBrevBehov.BEHOV_NAVN}"
                   ],
                   "htmlBase64": "$htmlBrevAsBase64",
                   "dokumentNavn": "vedtak.pdf",
                   "kontekst": "oppgave/$oppgaveId",
                   "oppgaveId": "$oppgaveId",
                   "ident": "${oppgave.ident}",
                   "sak": {
                      "id": "${sak.id}",
                      "kontekst": "${sak.kontekst}"
                  }
                }
                """.trimIndent()

            val pdfUrnString = "urn:pdf:123"
            rapid.sendTestMessage(arkiverbartDokumentBehovLøsning(oppgaveId, pdfUrnString))

            utsending = utsendingRepository.hent(oppgaveId)
            utsending.tilstand().type shouldBe AvventerJournalføring
            utsending.pdfUrn() shouldBe pdfUrnString.toUrn()
            rapid.inspektør.size shouldBe 2
            rapid.inspektør.message(1).toString() shouldEqualSpecifiedJson
                //language=JSON
                """
                {
                  "@event_name": "behov",
                  "@behov": [
                    "${JournalføringBehov.BEHOV_NAVN}"
                  ],
                  "tittel" : "Vedtak om dagpenger",
                  "ident": "${oppgave.ident}",
                  "pdfUrn": "$pdfUrnString",
                  "oppgaveId": "$oppgaveId",
                  "sak": {
                    "id": "${sak.id}",
                    "kontekst": "${sak.kontekst}"
                  }
                }
                """.trimIndent()

            val journalpostId = "123"
            rapid.sendTestMessage(journalføringBehovLøsning(oppgaveId, journalpostId))

            utsending = utsendingRepository.hent(oppgaveId)
            utsending.tilstand().type shouldBe AvventerDistribuering
            utsending.journalpostId() shouldBe journalpostId
            rapid.inspektør.size shouldBe 3
            rapid.inspektør.message(2).toString() shouldEqualSpecifiedJson
                //language=JSON
                """
                {
                  "@event_name": "behov",
                  "@behov": [
                    "${DistribueringBehov.BEHOV_NAVN}"
                  ],
                  "journalpostId": "${utsending.journalpostId()}",
                  "oppgaveId": "$oppgaveId"
                }
                """.trimIndent()

            val distribusjonId = "distribusjonId"
            rapid.sendTestMessage(
                distribuertDokumentBehovLøsning(
                    oppgaveId = oppgaveId,
                    journalpostId = journalpostId,
                    distribusjonId = distribusjonId,
                ),
            )
            utsending = utsendingRepository.hent(oppgaveId)
            utsending.tilstand().type shouldBe Distribuert
            utsending.distribusjonId() shouldBe distribusjonId
        }
    }
}
