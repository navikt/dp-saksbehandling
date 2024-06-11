package no.nav.dagpenger.saksbehandling

import io.kotest.assertions.json.shouldEqualSpecifiedJsonIgnoringOrder
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.db.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.db.lagBehandling
import no.nav.dagpenger.saksbehandling.db.lagOppgave
import no.nav.dagpenger.saksbehandling.mottak.UtsendingMottak
import no.nav.dagpenger.saksbehandling.utsending.ArkiverbartBrevBehov
import no.nav.dagpenger.saksbehandling.utsending.DistribueringBehov
import no.nav.dagpenger.saksbehandling.utsending.JournalføringBehov
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.AvventerArkiverbarVersjonAvBrev
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.AvventerDistribuering
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.AvventerJournalføring
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.Distribuert
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.VenterPåVedtak
import no.nav.dagpenger.saksbehandling.utsending.db.PostgresUtsendingRepository
import no.nav.dagpenger.saksbehandling.utsending.hendelser.VedtaksbrevHendelse
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import java.util.Base64
import java.util.UUID
import javax.sql.DataSource

class UtsendingMediatorTest {
    private val rapid = TestRapid()

    @Test
    fun `Vedtaksbrev starter utsendingen`() {
        withMigratedDb { datasource ->
            val oppgaveId = lagreOppgaveOgBehandling(datasource).first
            val utsendingRepository = PostgresUtsendingRepository(datasource)
            val utsendingMediator = UtsendingMediator(repository = utsendingRepository, rapidsConnection = rapid)

            val brev = "vedtaksbrev.html"
            val vedtaksbrevHendelse = VedtaksbrevHendelse(oppgaveId, brev)
            utsendingMediator.mottaBrev(vedtaksbrevHendelse)

            val utsending = utsendingRepository.hent(oppgaveId)
            utsending.oppgaveId shouldBe oppgaveId
            utsending.tilstand().type shouldBe VenterPåVedtak
            utsending.brev() shouldBe brev
        }
    }

    @Test
    fun `livssyklus av en utsending`() {
        withMigratedDb { datasource ->
            val (oppgaveId, behandlingId) = lagreOppgaveOgBehandling(datasource)

            val utsendingRepository = PostgresUtsendingRepository(datasource)
            val utsendingMediator = UtsendingMediator(repository = utsendingRepository, rapidsConnection = rapid)
            UtsendingMottak(
                rapidsConnection = rapid,
                utsendingMediator = utsendingMediator,
            )

            BehovLøsningMottak(
                utsendingMediator = utsendingMediator,
                rapidsConnection = rapid,
            )

            val htmlBrev = "<H1>Hei</H1><p>Her er et brev</p>"
            utsendingMediator.mottaBrev(VedtaksbrevHendelse(oppgaveId, htmlBrev))

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
            rapid.inspektør.message(0).toString() shouldEqualSpecifiedJsonIgnoringOrder
                //language=JSON
                """
                {
                   "@event_name": "behov",
                   "@behov": [
                     "${ArkiverbartBrevBehov.BEHOV_NAVN}"
                   ],
                   "html": "$htmlBrevAsBase64",
                   "oppgaveId": "$oppgaveId"
                }
                """.trimIndent()

            val pdfUrnString = "urn:pdf:123"
            rapid.sendTestMessage(arkiverbartDokumentBehovLosning(oppgaveId, pdfUrnString))

            utsending = utsendingRepository.hent(oppgaveId)
            utsending.tilstand().type shouldBe AvventerJournalføring
            utsending.pdfUrn() shouldBe pdfUrnString.toUrn()
            rapid.inspektør.size shouldBe 2
            rapid.inspektør.message(1).toString() shouldEqualSpecifiedJsonIgnoringOrder
                //language=JSON
                """
                {
                  "@event_name": "behov",
                  "@behov": [
                    "${JournalføringBehov.BEHOV_NAVN}"
                  ],
                  "pdfUrn": "$pdfUrnString",
                   "oppgaveId": "$oppgaveId"
                }
                """.trimIndent()

            val journalpostId = "123"
            rapid.sendTestMessage(journalføringBehovLøsning(oppgaveId, journalpostId))

            utsending = utsendingRepository.hent(oppgaveId)
            utsending.tilstand().type shouldBe AvventerDistribuering
            utsending.journalpostId() shouldBe journalpostId
            rapid.inspektør.size shouldBe 3
            rapid.inspektør.message(2).toString() shouldEqualSpecifiedJsonIgnoringOrder
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

            val distribueringId = "distribueringId"
            rapid.sendTestMessage(
                distribuertDokumentBehovLøsning(
                    oppgaveId = oppgaveId,
                    journalpostId = journalpostId,
                    distribueringId = distribueringId,
                ),
            )
            utsending = utsendingRepository.hent(oppgaveId)
            utsending.tilstand().type shouldBe Distribuert
            // utsending.distribueringId() shouldBe distribueringId
        }
    }

    private fun lagreOppgaveOgBehandling(dataSource: DataSource): Pair<UUID, UUID> {
        val behandling = lagBehandling()
        val oppgave = lagOppgave()
        val repository = PostgresOppgaveRepository(dataSource)
        repository.lagre(oppgave)
        return Pair(oppgave.oppgaveId, behandling.behandlingId)
    }

    private fun journalføringBehovLøsning(
        oppgaveUUID: UUID,
        journalpostId: String,
    ): String {
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

    private fun distribuertDokumentBehovLøsning(
        oppgaveId: UUID,
        journalpostId: String,
        distribueringId: String,
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
                  "distribueringId": "$distribueringId"
                }
              }
            }
            """.trimIndent()
    }

    //language=JSON
    private fun arkiverbartDokumentBehovLosning(
        oppgaveUUID: UUID,
        pdfUrnString: String,
    ) = """
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
