package no.nav.dagpenger.saksbehandling

import io.kotest.assertions.json.shouldContainJsonKey
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.hendelser.UtsendingDistribuert
import no.nav.dagpenger.saksbehandling.klage.OpplysningType
import no.nav.dagpenger.saksbehandling.klage.Verdi
import no.nav.dagpenger.saksbehandling.modell.helpers.TestHelpers
import org.junit.jupiter.api.Test
import java.util.UUID

class KlageInstansBehovByggerTest {
    private val sakId = UUID.randomUUID()
    private val utsendingId = UUID.randomUUID()
    private val journalpostIdForOpprinneligVedtak = "originalJournalpostId"

    @Test
    fun `bygger behov for klageinstans med minimal data`() {
        val klageBehandling =
            TestHelpers.Klage.lagKlageBehandling(
                journalpostId = "klageJournalpostId",
                behandlendeEnhet = "4450",
            )

        val hendelse =
            UtsendingDistribuert(
                behandlingId = klageBehandling.behandlingId,
                utsendingId = utsendingId,
                ident = TestHelpers.Person.personIdent,
                journalpostId = "vedtakJournalpostId",
                distribusjonId = UUID.randomUUID().toString(),
            )

        val bygger =
            KlageInstansBehovBygger(
                klageBehandling = klageBehandling,
                sakId = sakId,
                hendelse = hendelse,
                finnJournalpostIdForBehandling = { null },
            )

        val behov = bygger.behov

        behov shouldContainJsonKey "@behov"
        behov shouldEqualJson """
            {
              "@behov": ["OversendelseKlageinstans"],
              "behandlingId": "${klageBehandling.behandlingId}",
              "ident": "${TestHelpers.Person.personIdent}",
              "sakId": "$sakId",
              "behandlendeEnhet": "4450",
              "journalpostId": "vedtakJournalpostId",
              "tilknyttedeJournalposter": ["klageJournalpostId"]
            }
        """
    }

    @Test
    fun `bygger behov med alle tilknyttede journalposter`() {
        val klageBehandling =
            TestHelpers.Klage.lagKlageBehandling(
                journalpostId = "klageJournalpostId",
            )

        val hendelse =
            UtsendingDistribuert(
                behandlingId = klageBehandling.behandlingId,
                utsendingId = utsendingId,
                ident = TestHelpers.Person.personIdent,
                journalpostId = "vedtakJournalpostId",
                distribusjonId = UUID.randomUUID().toString(),
            )

        val bygger =
            KlageInstansBehovBygger(
                klageBehandling = klageBehandling,
                sakId = sakId,
                hendelse = hendelse,
                finnJournalpostIdForBehandling = { journalpostIdForOpprinneligVedtak },
            )

        val behov = bygger.behov
        behov shouldEqualJson """
            {
              "@behov": ["OversendelseKlageinstans"],
              "behandlingId": "${klageBehandling.behandlingId}",
              "ident": "${TestHelpers.Person.personIdent}",
              "sakId": "$sakId",
              "behandlendeEnhet": "4409",
              "journalpostId": "vedtakJournalpostId",
              "tilknyttedeJournalposter": ["vedtakJournalpostId", "klageJournalpostId", "$journalpostIdForOpprinneligVedtak"]
            }
        """
    }

    @Test
    fun `bygger behov med fullmektig data`() {
        val klageBehandling =
            TestHelpers.Klage.lagKlageBehandling(
                opplysninger =
                    setOf(
                        no.nav.dagpenger.saksbehandling.klage.Opplysning(
                            type = OpplysningType.FULLMEKTIG_NAVN,
                            verdi = Verdi.TekstVerdi("Advokat Hansen"),
                        ),
                        no.nav.dagpenger.saksbehandling.klage.Opplysning(
                            type = OpplysningType.FULLMEKTIG_ADRESSE_1,
                            verdi = Verdi.TekstVerdi("Advokateveien 1"),
                        ),
                        no.nav.dagpenger.saksbehandling.klage.Opplysning(
                            type = OpplysningType.FULLMEKTIG_POSTNR,
                            verdi = Verdi.TekstVerdi("0123"),
                        ),
                        no.nav.dagpenger.saksbehandling.klage.Opplysning(
                            type = OpplysningType.FULLMEKTIG_POSTSTED,
                            verdi = Verdi.TekstVerdi("Oslo"),
                        ),
                    ),
            )

        val hendelse =
            UtsendingDistribuert(
                behandlingId = klageBehandling.behandlingId,
                utsendingId = utsendingId,
                ident = TestHelpers.Person.personIdent,
                journalpostId = "vedtakJournalpostId",
                distribusjonId = UUID.randomUUID().toString(),
            )

        val bygger =
            KlageInstansBehovBygger(
                klageBehandling = klageBehandling,
                sakId = sakId,
                hendelse = hendelse,
                finnJournalpostIdForBehandling = { null },
            )

        val behov = bygger.behov
        behov shouldEqualJson """
            {
              "@behov": ["OversendelseKlageinstans"],
              "behandlingId": "${klageBehandling.behandlingId}",
              "ident": "${TestHelpers.Person.personIdent}",
              "sakId": "$sakId",
              "behandlendeEnhet": "4409",
              "journalpostId": "vedtakJournalpostId",
              "tilknyttedeJournalposter": ["vedtakJournalpostId"],
              "fullmektig": {
                "navn": "Advokat Hansen",
                "adresse1": "Advokateveien 1",
                "postnummer": "0123",
                "poststed": "Oslo"
              }
            }
        """
    }

    @Test
    fun `bruker default behandlende enhet 4409 når ikke satt`() {
        val klageBehandling =
            TestHelpers.Klage.lagKlageBehandling(
                behandlendeEnhet = null,
            )

        val hendelse =
            UtsendingDistribuert(
                behandlingId = klageBehandling.behandlingId,
                utsendingId = utsendingId,
                ident = TestHelpers.Person.personIdent,
                journalpostId = "vedtakJournalpostId",
                distribusjonId = UUID.randomUUID().toString(),
            )

        val bygger =
            KlageInstansBehovBygger(
                klageBehandling = klageBehandling,
                sakId = sakId,
                hendelse = hendelse,
                finnJournalpostIdForBehandling = { null },
            )

        bygger.behov shouldContainJsonKey "behandlendeEnhet"
        bygger.behov shouldEqualJson """
            {
              "@behov": ["OversendelseKlageinstans"],
              "behandlingId": "${klageBehandling.behandlingId}",
              "ident": "${TestHelpers.Person.personIdent}",
              "sakId": "$sakId",
              "behandlendeEnhet": "4409",
              "journalpostId": "vedtakJournalpostId",
              "tilknyttedeJournalposter": ["vedtakJournalpostId"]
            }
        """
    }

    @Test
    fun `behov navn er OversendelseKlageinstans`() {
        KlageInstansBehovBygger.BEHOV_NAVN shouldBe "OversendelseKlageinstans"
    }
}
