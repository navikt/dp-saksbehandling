package no.nav.dagpenger.saksbehandling.utsending

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.assertions.json.shouldEqualSpecifiedJson
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.BehandlingType
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.UtsendingSak
import no.nav.dagpenger.saksbehandling.db.DBTestHelper
import no.nav.dagpenger.saksbehandling.helper.arkiverbartDokumentBehovLøsning
import no.nav.dagpenger.saksbehandling.helper.distribuertDokumentBehovLøsning
import no.nav.dagpenger.saksbehandling.helper.journalføringBehovLøsning
import no.nav.dagpenger.saksbehandling.helper.lagreOppgave
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
import no.nav.dagpenger.saksbehandling.lagPerson
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
import java.time.LocalDateTime
import java.util.Base64

class UtsendingMediatorTest {
    private val rapid = TestRapid()

    @Test
    fun `livssyklus for en utsending`() {
        val behandling =
            Behandling(
                behandlingId = UUIDv7.ny(),
                type = BehandlingType.KLAGE,
                opprettet = LocalDateTime.now(),
                hendelse = TomHendelse,
            )
        val person = lagPerson()

        DBTestHelper.withBehandling(behandling = behandling, person = person) { ds ->

            val oppgave = lagreOppgave(dataSource = ds, behandlingId = behandling.behandlingId, personIdent = person.ident)

            val oppgaveId = oppgave.oppgaveId
            val behandlingId = oppgave.behandlingId

            val utsendingRepository = PostgresUtsendingRepository(ds)
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
            utsendingMediator.opprettUtsending(
                oppgaveId = oppgaveId,
                brev = htmlBrev,
                ident = oppgave.personIdent(),
                type = UtsendingType.KLAGEMELDING,
            )

            var utsending = utsendingRepository.hent(oppgaveId)
            utsending.oppgaveId shouldBe oppgaveId
            utsending.tilstand().type shouldBe VenterPåVedtak
            utsending.brev() shouldBe htmlBrev

            val utsendingSak = UtsendingSak("sakId", "fagsystem")
            rapid.sendTestMessage(
                //language=JSON
                """
                {
                    "@event_name": "start_utsending",
                    "oppgaveId": "$oppgaveId",
                    "behandlingId": "$behandlingId",
                    "ident": "12345678901",
                    "sak": {
                        "id": "${utsendingSak.id}",
                        "kontekst": "${utsendingSak.kontekst}"
                    }
                }
                """,
            )

            utsending = utsendingRepository.hent(oppgaveId)
            utsending.sak() shouldBe utsendingSak
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
                   "ident": "${oppgave.personIdent()}",
                   "sak": {
                      "id": "${utsendingSak.id}",
                      "kontekst": "${utsendingSak.kontekst}"
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
                  "tittel" : "${UtsendingType.KLAGEMELDING.brevTittel}",
                  "skjemaKode" : "${UtsendingType.KLAGEMELDING.skjemaKode}",
                  "ident": "${oppgave.personIdent()}",
                  "pdfUrn": "$pdfUrnString",
                  "oppgaveId": "$oppgaveId",
                  "sak": {
                    "id": "${utsendingSak.id}",
                    "kontekst": "${utsendingSak.kontekst}"
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
